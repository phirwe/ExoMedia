package com.example.poorwahirve.exomedia

import android.support.v4.media.session.MediaSessionCompat
import com.google.android.exoplayer2.SimpleExoPlayer

class SimpleSessionCallback(val simpleExoPlayer: SimpleExoPlayer) : MediaSessionCompat.Callback() {
    override fun onPlay() {
        simpleExoPlayer.setPlayWhenReady(true)
    }

    override fun onPause() {
        simpleExoPlayer.setPlayWhenReady(false)
    }

    override fun onSkipToPrevious() {
        simpleExoPlayer.seekTo(0)
    }
}