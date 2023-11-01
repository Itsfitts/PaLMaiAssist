package ai.assist.palmai.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import ai.assist.palmai.app.domain.model.Chat
import ai.assist.palmai.app.domain.model.Message

const val DB_NAME = "messages_database"

@Database(entities = [Message::class, Chat::class], version = 1)
abstract class MessagesDatabase: RoomDatabase() {

    abstract fun messagesDao(): MessagesDao
}
