package com.mycompany.lovegarden

data class ChatMessage(
    val sender: String = "",
    val text: String = "",
    val timestamp: Long = 0L,
    val senderUid: String = ""  // Firebase Auth의 UID 저장
) 