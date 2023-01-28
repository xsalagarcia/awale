package sala.xevi.awale.models

import android.os.Parcel
import android.os.Parcelable
import sala.xevi.awale.R
import java.io.DataInput
import java.io.Externalizable
import java.io.ObjectInput
import java.io.ObjectOutput

class Player () : Parcelable, Externalizable {

    lateinit var name: String
    var score: Int = 0
    var level: Levels = Levels.HUMAN
    var timeLeft: Int = Int.MAX_VALUE
    var previousScore = score

    //parcelable
    constructor(parcel: Parcel) : this() {
        name = parcel.readString()!!
        score = parcel.readInt()
        level = Levels.values()[ parcel.readInt()]
        previousScore = parcel.readInt()
        timeLeft = parcel.readInt()
    }

    constructor(name: String): this () {
        this.name = name
        this.score = score
    }

    constructor(name: String, score: Int): this() {
        this.name = name
        this.score = score
    }


    /**
     * Enum class. The strings will be array-levels
     */
    enum class Levels {
        HUMAN,
        SILLY,
        NOT_SO_SILLY,
        INTERMEDIATE,
        SMART,
        VERY_SMART,
        WONT_WIN;

    } //resources.getStringArray(R.array.array_levels)[Player.Levels.VERY_SMART.ordinal] <-- will get the number, and we have an array in R.strings






    //parcelable
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeInt(score)
        parcel.writeInt(level.ordinal)
        parcel.writeInt(previousScore)
        parcel.writeInt(timeLeft)
    }

    //parcelable
    override fun describeContents(): Int {
        return 0
    }

    //parcelable
    companion object CREATOR : Parcelable.Creator<Player> {
        override fun createFromParcel(parcel: Parcel): Player {
            return Player(parcel)
        }

        override fun newArray(size: Int): Array<Player?> {
            return arrayOfNulls(size)
        }
    }

    override fun writeExternal(out: ObjectOutput?) {
        out!!.apply {
            writeUTF(name)
            writeInt(score)
            writeInt(level.ordinal)
            writeInt(timeLeft)
            writeInt(previousScore)
        }

    }

    override fun readExternal(oi:  ObjectInput?) {
        oi!!.apply {
            name = readUTF()
            score = readInt()
            level = Levels.values()[readInt()]
            timeLeft = readInt()
            previousScore = readInt()
        }
    }

}