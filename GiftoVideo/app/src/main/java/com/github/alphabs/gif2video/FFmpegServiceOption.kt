package com.github.alphabs.gif2video

import android.os.Parcel
import android.os.Parcelable

class FFmpegServiceOption() : Parcelable {
    var targetDir: String = "";
    var targetExtension: String = "";
    var ffmpegArguments: String = "";
    var outputExtension: String = "";

    var preserveFileDate: Boolean = false;
    var removeCompletedFile : Boolean = false;

    constructor(parcel: Parcel) : this() {
        targetDir = parcel.readString() ?: ""
        targetExtension = parcel.readString() ?: ""
        ffmpegArguments = parcel.readString() ?: ""
        outputExtension = parcel.readString() ?: ""
        preserveFileDate = parcel.readByte() != 0.toByte()
        removeCompletedFile = parcel.readByte() != 0.toByte()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(targetDir)
        parcel.writeString(targetExtension)
        parcel.writeString(ffmpegArguments)
        parcel.writeString(outputExtension)
        parcel.writeByte(if (preserveFileDate) 1 else 0)
        parcel.writeByte(if (removeCompletedFile) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<FFmpegServiceOption> {
        override fun createFromParcel(parcel: Parcel): FFmpegServiceOption {
            return FFmpegServiceOption(parcel)
        }

        override fun newArray(size: Int): Array<FFmpegServiceOption?> {
            return arrayOfNulls(size)
        }
    }
}