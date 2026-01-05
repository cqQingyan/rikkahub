package me.rerere.rikkahub.data.db.migrations

import androidx.room.DeleteColumn
import androidx.room.migration.AutoMigrationSpec

@DeleteColumn(tableName = "ConversationEntity", columnName = "is_pinned")
class Migration_12_13 : AutoMigrationSpec
