package ar.edu.itba.harmos.exceptions

class UnauthorizedNotificationAccessException(notificationId: Long, userId: Long) : 
    RuntimeException("User $userId is not authorized to access notification $notificationId") 