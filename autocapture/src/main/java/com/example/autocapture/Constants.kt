package com.example.autocapture


object ManualCaptureStatus{
    const val FRONT_CAPTURE = 0
    const val FRONT_CAPTURE_DONE = 1
    const val BACK_CAPTURE = 2
    const val BACK_CAPTURE_DONE = 4
}

object CardFace{
    const val FRONT = 0
    const val BACK = 1
}

object CardImage{
    const val FRONT_CARD_IMAGE= "front_card.jpg"
    const val BACK_CARD_IMAGE= "back_card.jpg"
}

object CardCaptureStatus{
    const val START = -1
    const val NO_CARD = 0
    const val CARD_NOT_FIT = 1
    const val CARD_TOO_FAR = 2
    const val CARD_NO_FACE = 3
    const val CARD_BACK_HAS_FACE = 4
    const val CARD_OK = 5
    const val CARD_TOO_NEAR = 6
    const val CARD_DONE = 10
}