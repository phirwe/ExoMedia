package com.example.poorwahirve.exomedia

import android.app.*
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import kotlinx.android.synthetic.main.activity_main.*
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.support.v4.app.NotificationCompat
import android.support.v4.media.session.MediaButtonReceiver
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import com.example.poorwahirve.exomedia.R.drawable.*
import io.reactivex.Observable
import kotlinx.android.synthetic.main.exo_playback_control_view.*


class MainActivity : AppCompatActivity(), Player.EventListener {

    private val MSESSION = "media_session"

    companion object {

        private lateinit var mMediaSession : MediaSessionCompat

        class MediaReceiver : BroadcastReceiver() {

            override fun onReceive(context: Context?, intent: Intent?) {
                MediaButtonReceiver.handleIntent(mMediaSession, intent)
            }

        }

    }

    private lateinit var simpleExoPlayer : SimpleExoPlayer
    private lateinit var mStateBuilder : PlaybackStateCompat.Builder
    private lateinit var mNotificationManager : NotificationManager
    private lateinit var token : MediaSessionCompat.Token
    private lateinit var fullScreenDialog : Dialog
    private var fullScreen = false
    private lateinit var mainFrameLayout : FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initPlayer()
        initFullscreen()
        initPlayerView()

    }




    private fun initPlayerView() {
        simpleExoPlayerView.defaultArtwork = BitmapFactory.decodeResource(resources, R.drawable.exo_controls_play)

        mMediaSession = MediaSessionCompat(this, this.javaClass.simpleName)


        mMediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)

        mMediaSession.setMediaButtonReceiver(null) // doesn't allow app to restart player

        mStateBuilder = PlaybackStateCompat.Builder().setActions(
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_REWIND or
                PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_FAST_FORWARD
        )

        mMediaSession.setPlaybackState(mStateBuilder.build())
        mMediaSession.setCallback(SimpleSessionCallback(simpleExoPlayer))
        mMediaSession.isActive = true

    }

    private fun initPlayer() {

        val trackSelector = DefaultTrackSelector()




        simpleExoPlayer = ExoPlayerFactory.newSimpleInstance(
                this,
                trackSelector
        )

        simpleExoPlayerView.player = simpleExoPlayer
        val mediaSource = ExtractorMediaSource(Uri.parse("http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4"),
                DefaultDataSourceFactory(this, "ua"), DefaultExtractorsFactory(), null, null)

        simpleExoPlayer.prepare(mediaSource)
        simpleExoPlayer.playWhenReady = true
        simpleExoPlayer.addListener(this)



    }

    private fun initFullscreen() {
        initFullScreenButton()
        fullScreenDialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val dialogObservable : Observable<Dialog> = Observable
                .just(fullScreenDialog)
                .doOnNext {
                    it.onBackPressed().takeIf { fullScreen }
                }
        dialogObservable.subscribe()


    }

    private fun openFullScreenDialog() {

        (simpleExoPlayerView.parent as ViewGroup).removeView(simpleExoPlayerView)
        fullScreenDialog.addContentView(simpleExoPlayerView, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        exo_fullscreen_icon.setImageDrawable(getDrawable(ic_fullscreen_skrink))
        fullScreen = true
        fullScreenDialog.show()

    }

    private fun closeFullScreenDialog() {

        (simpleExoPlayerView.parent as ViewGroup).removeView(simpleExoPlayerView)
        main_layout.addView(simpleExoPlayerView)
        fullScreen = false
        fullScreenDialog.dismiss()
        exo_fullscreen_icon.setImageDrawable(getDrawable(ic_fullscreen_expand))

    }

    private fun initFullScreenButton() {

        val fullScreenIconObservable : Observable<ImageView> = Observable
                .just(exo_fullscreen_icon)
                .doOnNext {
                    it.setOnClickListener {
                        if(fullScreen)
                            closeFullScreenDialog()
                        else
                            openFullScreenDialog()
                    }
                }

        fullScreenIconObservable.subscribe()

    }


    override fun onDestroy() {
        releasePlayer()
        mMediaSession.isActive = false
        super.onDestroy()
    }

    private fun releasePlayer() {
        mNotificationManager.cancelAll()
        simpleExoPlayer.stop()
        simpleExoPlayer.release()
    }

    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {

    }

    override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {

    }

    override fun onPlayerError(error: ExoPlaybackException?) {

    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        if(playWhenReady) {
            mStateBuilder.setState(PlaybackStateCompat.STATE_PLAYING,
                    simpleExoPlayer.currentPosition, 1f)
            Log.e("Player State Changed", "Playing")
        } else {
            mStateBuilder.setState(PlaybackStateCompat.STATE_PAUSED,
                    simpleExoPlayer.currentPosition, 1f)
            Log.e("Player State Changed", "Paused")
        }
        mMediaSession.setPlaybackState(mStateBuilder.build())
        showNotification(mStateBuilder.build())
    }

    private fun showNotification(build: PlaybackStateCompat?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel()
        }

        val builder = NotificationCompat.Builder(this, MSESSION)

        var icon : Int
        var play_pause : String

        if(build?.state == PlaybackStateCompat.STATE_PLAYING) {
            icon = exo_controls_pause
            play_pause = "Pause"
        }
        else {
            icon = exo_controls_play
            play_pause = "Play"
        }



        val playPauseAction = NotificationCompat.Action(
                icon, play_pause,
                MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY_PAUSE)
        )

        val contentPendingIntent = PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                0
        )

        token = mMediaSession.sessionToken

        builder.setContentTitle("Elephant Video")
                .setContentText(getString(R.string.app_name))
                .setContentIntent(contentPendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .addAction(playPauseAction)
                .setStyle(android.support.v4.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(token)
                        .setShowActionsInCompactView(0))

        mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mNotificationManager.notify(1, builder.build())

    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel() {
        val mNotificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val id = MSESSION
        val name : CharSequence = "MediaPlayback"
        val description = "MediaPlaybackControls"
        val importance = NotificationManager.IMPORTANCE_LOW
        val mChannel = NotificationChannel(id, name, importance)
        mChannel.description = description
        mChannel.setShowBadge(false)
        mChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        mNotificationManager.createNotificationChannel(mChannel)
    }

    override fun onLoadingChanged(isLoading: Boolean) {

    }

    override fun onPositionDiscontinuity() {

    }

    override fun onRepeatModeChanged(repeatMode: Int) {

    }

    override fun onTimelineChanged(timeline: Timeline?, manifest: Any?) {

    }

}


