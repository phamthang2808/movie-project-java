package com.example.thangcachep.movie_project_be.utils;

/**
 * Message Keys cho i18n
 * Tất cả các message key được định nghĩa ở đây để dễ quản lý
 */
public class MessageKeys {

    // ==========================================
    // AUTH MESSAGES
    // ==========================================
    public static final String LOGIN_SUCCESS = "auth.login.success";
    public static final String LOGIN_FAILED = "auth.login.failed";
    public static final String REGISTER_SUCCESS = "auth.register.success";
    public static final String LOGOUT_SUCCESS = "auth.logout.success";
    public static final String EMAIL_VERIFY_SUCCESS = "auth.email.verify.success";
    public static final String GOOGLE_LOGIN_SUCCESS = "auth.google.login.success";
    public static final String WRONG_EMAIL_PASSWORD = "auth.wrong.email.password";
    public static final String USER_LOCKED = "auth.user.locked";
    public static final String PASSWORD_NOT_MATCH = "auth.password.not.match";

    // ==========================================
    // EXCEPTION MESSAGES
    // ==========================================
    public static final String DATA_NOT_FOUND = "exception.data.not.found";
    public static final String INVALID_PARAM = "exception.invalid.param";
    public static final String PERMISSION_DENIED = "exception.permission.denied";
    public static final String UNAUTHORIZED = "exception.unauthorized";
    public static final String BAD_REQUEST = "exception.bad.request";
    public static final String CONFLICT = "exception.conflict";
    public static final String VALIDATION_ERROR = "exception.validation.error";
    public static final String INTERNAL_SERVER_ERROR = "exception.internal.server.error";
    public static final String RUNTIME_ERROR = "exception.runtime.error";

    // ==========================================
    // CATEGORY MESSAGES
    // ==========================================
    public static final String CATEGORY_GET_ALL_SUCCESS = "category.get.all.success";
    public static final String CATEGORY_GET_SUCCESS = "category.get.success";
    public static final String CATEGORY_CREATE_SUCCESS = "category.create.success";
    public static final String CATEGORY_UPDATE_SUCCESS = "category.update.success";
    public static final String CATEGORY_DELETE_SUCCESS = "category.delete.success";

    // ==========================================
    // MOVIE MESSAGES
    // ==========================================
    public static final String MOVIE_GET_ALL_SUCCESS = "movie.get.all.success";
    public static final String MOVIE_GET_SUCCESS = "movie.get.success";
    public static final String MOVIE_CREATE_SUCCESS = "movie.create.success";
    public static final String MOVIE_UPDATE_SUCCESS = "movie.update.success";
    public static final String MOVIE_DELETE_SUCCESS = "movie.delete.success";
    public static final String MOVIE_APPROVE_SUCCESS = "movie.approve.success";
    public static final String MOVIE_REJECT_SUCCESS = "movie.reject.success";
    public static final String MOVIE_GET_COMMENTS_SUCCESS = "movie.get.comments.success";
    public static final String MOVIE_GET_EPISODES_SUCCESS = "movie.get.episodes.success";
    public static final String MOVIE_SEARCH_SUCCESS = "movie.search.success";
    public static final String MOVIE_GET_TRENDING_SUCCESS = "movie.get.trending.success";
    public static final String MOVIE_GET_TOP_WEEK_SUCCESS = "movie.get.top.week.success";
    public static final String MOVIE_GET_BY_CATEGORY_SUCCESS = "movie.get.by.category.success";
    public static final String MOVIE_GET_RECOMMENDATIONS_SUCCESS = "movie.get.recommendations.success";
    public static final String MOVIE_ADD_FAVORITE_SUCCESS = "movie.add.favorite.success";
    public static final String MOVIE_REMOVE_FAVORITE_SUCCESS = "movie.remove.favorite.success";
    public static final String MOVIE_GET_FAVORITES_SUCCESS = "movie.get.favorites.success";
    public static final String MOVIE_ADD_WATCHLIST_SUCCESS = "movie.add.watchlist.success";
    public static final String MOVIE_REMOVE_WATCHLIST_SUCCESS = "movie.remove.watchlist.success";
    public static final String MOVIE_GET_WATCHLIST_SUCCESS = "movie.get.watchlist.success";
    public static final String MOVIE_GET_HISTORY_SUCCESS = "movie.get.history.success";
    public static final String MOVIE_UPDATE_PROGRESS_SUCCESS = "movie.update.progress.success";
    public static final String MOVIE_LIKE_COMMENT_SUCCESS = "movie.like.comment.success";

    // ==========================================
    // COMMENT MESSAGES
    // ==========================================
    public static final String COMMENT_CREATE_SUCCESS = "comment.create.success";
    public static final String COMMENT_UPDATE_SUCCESS = "comment.update.success";
    public static final String COMMENT_DELETE_SUCCESS = "comment.delete.success";
    public static final String COMMENT_APPROVE_SUCCESS = "comment.approve.success";
    public static final String COMMENT_REJECT_SUCCESS = "comment.reject.success";
    public static final String COMMENT_GET_SUCCESS = "comment.get.success";

    // ==========================================
    // USER MESSAGES
    // ==========================================
    public static final String USER_GET_PROFILE_SUCCESS = "user.get.profile.success";
    public static final String USER_UPDATE_PROFILE_SUCCESS = "user.update.profile.success";
    public static final String USER_CHANGE_PASSWORD_SUCCESS = "user.change.password.success";
    public static final String USER_UPLOAD_AVATAR_SUCCESS = "user.upload.avatar.success";
    public static final String USER_DELETE_AVATAR_SUCCESS = "user.delete.avatar.success";

    // ==========================================
    // PAYMENT MESSAGES
    // ==========================================
    public static final String PAYMENT_VIP_UPGRADE_SUCCESS = "payment.vip.upgrade.success";
    public static final String PAYMENT_PAYPAL_CREATE_SUCCESS = "payment.paypal.create.success";
    public static final String PAYMENT_PAYPAL_SUCCESS = "payment.paypal.success";
    public static final String PAYMENT_PAYPAL_CANCELLED = "payment.paypal.cancelled";
    public static final String PAYMENT_VNPAY_CREATE_SUCCESS = "payment.vnpay.create.success";
    public static final String PAYMENT_BANK_TRANSFER_CREATE_SUCCESS = "payment.bank.transfer.create.success";
    public static final String PAYMENT_BANK_TRANSFER_VERIFY_SUCCESS = "payment.bank.transfer.verify.success";

    // ==========================================
    // UPLOAD MESSAGES
    // ==========================================
    public static final String UPLOAD_VIDEO_SUCCESS = "upload.video.success";
    public static final String UPLOAD_IMAGE_SUCCESS = "upload.image.success";
    public static final String UPLOAD_DOCUMENT_SUCCESS = "upload.document.success";
    public static final String UPLOAD_FILE_SUCCESS = "upload.file.success";
    public static final String UPLOAD_MULTIPLE_SUCCESS = "upload.multiple.success";
    public static final String UPLOAD_DELETE_SUCCESS = "upload.delete.success";
    public static final String UPLOAD_FILE_EMPTY = "upload.file.empty";
    public static final String UPLOAD_FILE_INVALID_EXTENSION = "upload.file.invalid.extension";

    // ==========================================
    // CHAT MESSAGES
    // ==========================================
    public static final String CHAT_SEND_SUCCESS = "chat.send.success";

    // ==========================================
    // WATCH TOGETHER MESSAGES
    // ==========================================
    public static final String WATCH_TOGETHER_ROOM_CREATE_SUCCESS = "watch.together.room.create.success";
    public static final String WATCH_TOGETHER_ROOM_JOIN_SUCCESS = "watch.together.room.join.success";
    public static final String WATCH_TOGETHER_ROOM_LEAVE_SUCCESS = "watch.together.room.leave.success";
    public static final String WATCH_TOGETHER_ROOM_GET_ALL_SUCCESS = "watch.together.room.get.all.success";

    // ==========================================
    // STATISTICS MESSAGES
    // ==========================================
    public static final String STATISTICS_GET_SUCCESS = "statistics.get.success";

    // ==========================================
    // HEALTH MESSAGES
    // ==========================================
    public static final String HEALTH_CHECK_SUCCESS = "health.check.success";

    // ==========================================
    // ADDITIONAL MESSAGES
    // ==========================================
    public static final String SUCCESS = "success";
    public static final String OTP_SEND_SUCCESS = "otp.send.success";
    public static final String OTP_VERIFY_SUCCESS = "otp.verify.success";
    public static final String PASSWORD_RESET_SUCCESS = "password.reset.success";

    // ==========================================
    // OLD MESSAGE KEYS (for backward compatibility)
    // ==========================================
    @Deprecated
    public static final String LOGIN_SUCCESSFULLY = "user.login.login_successfully";
    @Deprecated
    public static final String REGISTER_SUCCESSFULLY = "user.login.register_successfully";
    @Deprecated
    public static final String LOGIN_FAILED_OLD = "user.login.login_failed";
    @Deprecated
    public static final String WRONG_PHONE_PASSWORD = "user.login.wrong_phone_password";
    @Deprecated
    public static final String ROLE_DOES_NOT_EXISTS = "user.login.role_not_exist";
    @Deprecated
    public static final String USER_IS_LOCKED = "user.login.user_is_locked";
}
