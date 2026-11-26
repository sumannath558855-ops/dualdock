package com.example.data

data class DualAppPreset(
    val key: String,
    val name: String,
    val category: String,
    val url: String,
    val knownPackageNames: List<String>,
    val monogram: String,
    val brandColorHex: Long,
    val defaultDesktopUa: Boolean = false,
    val helperHint: String
)

object CloneCatalog {
    val presets: List<DualAppPreset> = listOf(
        DualAppPreset(
            key = "whatsapp",
            name = "WhatsApp",
            category = "Messaging",
            url = "https://web.whatsapp.com",
            knownPackageNames = listOf("com.whatsapp", "com.whatsapp.w4b"),
            monogram = "WA",
            brandColorHex = 0xFF25D366,
            defaultDesktopUa = true,
            helperHint = "Link a second WhatsApp account via QR or phone number in desktop container mode."
        ),
        DualAppPreset(
            key = "flipkart",
            name = "Flipkart",
            category = "Shopping",
            url = "https://www.flipkart.com",
            knownPackageNames = listOf("com.flipkart.android"),
            monogram = "FK",
            brandColorHex = 0xFF2874F0,
            defaultDesktopUa = false,
            helperHint = "Run a separate Flipkart shopping account with isolated cookies and cart."
        ),
        DualAppPreset(
            key = "myntra",
            name = "Myntra",
            category = "Shopping",
            url = "https://www.myntra.com",
            knownPackageNames = listOf("com.myntra.android"),
            monogram = "MY",
            brandColorHex = 0xFFFF3F6C,
            defaultDesktopUa = false,
            helperHint = "Separate Myntra login & wishlist. Use Work Space if you need the native Myntra app's WhatsApp OTP."
        ),
        DualAppPreset(
            key = "amazon",
            name = "Amazon",
            category = "Shopping",
            url = "https://www.amazon.in",
            knownPackageNames = listOf("in.amazon.mShop.android.shopping", "com.amazon.mShop.android.shopping"),
            monogram = "AZ",
            brandColorHex = 0xFFFF9900,
            defaultDesktopUa = false,
            helperHint = "Sign into a separate Amazon Prime or business buyer account."
        ),
        DualAppPreset(
            key = "instagram",
            name = "Instagram",
            category = "Social",
            url = "https://www.instagram.com",
            knownPackageNames = listOf("com.instagram.android"),
            monogram = "IG",
            brandColorHex = 0xFFE1306C,
            defaultDesktopUa = false,
            helperHint = "Isolated Instagram feed, reels, and DMs in its own session."
        ),
        DualAppPreset(
            key = "telegram",
            name = "Telegram",
            category = "Messaging",
            url = "https://web.telegram.org/a/",
            knownPackageNames = listOf("org.telegram.messenger", "org.telegram.messenger.web"),
            monogram = "TG",
            brandColorHex = 0xFF26A5E4,
            defaultDesktopUa = false,
            helperHint = "Run an extra Telegram account via Telegram Web."
        ),
        DualAppPreset(
            key = "meesho",
            name = "Meesho",
            category = "Shopping",
            url = "https://www.meesho.com",
            knownPackageNames = listOf("com.meesho.supply"),
            monogram = "MS",
            brandColorHex = 0xFF9F2089,
            defaultDesktopUa = false,
            helperHint = "Manage a second Meesho buyer or reseller login independently."
        ),
        DualAppPreset(
            key = "ajio",
            name = "AJIO",
            category = "Shopping",
            url = "https://www.ajio.com",
            knownPackageNames = listOf("com.ril.ajio"),
            monogram = "AJ",
            brandColorHex = 0xFF2C4152,
            defaultDesktopUa = false,
            helperHint = "Separate AJIO fashion account and bag."
        ),
        DualAppPreset(
            key = "facebook",
            name = "Facebook",
            category = "Social",
            url = "https://m.facebook.com",
            knownPackageNames = listOf("com.facebook.katana", "com.facebook.lite"),
            monogram = "FB",
            brandColorHex = 0xFF1877F2,
            defaultDesktopUa = false,
            helperHint = "Keep work pages or an alternate Facebook profile separated."
        ),
        DualAppPreset(
            key = "x",
            name = "X",
            category = "Social",
            url = "https://x.com",
            knownPackageNames = listOf("com.twitter.android"),
            monogram = "X",
            brandColorHex = 0xFF334155,
            defaultDesktopUa = false,
            helperHint = "Separate X / Twitter account in an isolated container."
        ),
        DualAppPreset(
            key = "gmail",
            name = "Gmail",
            category = "Productivity",
            url = "https://mail.google.com",
            knownPackageNames = listOf("com.google.android.gm"),
            monogram = "GM",
            brandColorHex = 0xFFEA4335,
            defaultDesktopUa = false,
            helperHint = "Isolated inbox that does not sync into your phone's system accounts."
        ),
        DualAppPreset(
            key = "swiggy",
            name = "Swiggy",
            category = "Delivery",
            url = "https://www.swiggy.com",
            knownPackageNames = listOf("in.swiggy.android"),
            monogram = "SW",
            brandColorHex = 0xFFFC8019,
            defaultDesktopUa = false,
            helperHint = "Separate Swiggy food & Instamart account."
        ),
        DualAppPreset(
            key = "zomato",
            name = "Zomato",
            category = "Delivery",
            url = "https://www.zomato.com",
            knownPackageNames = listOf("com.application.zomato"),
            monogram = "ZM",
            brandColorHex = 0xFFCB202D,
            defaultDesktopUa = false,
            helperHint = "Independent Zomato ordering account."
        ),
        DualAppPreset(
            key = "youtube",
            name = "YouTube",
            category = "Media",
            url = "https://m.youtube.com",
            knownPackageNames = listOf("com.google.android.youtube"),
            monogram = "YT",
            brandColorHex = 0xFFFF0000,
            defaultDesktopUa = false,
            helperHint = "Separate YouTube subscriptions and watch history."
        ),
        DualAppPreset(
            key = "linkedin",
            name = "LinkedIn",
            category = "Productivity",
            url = "https://www.linkedin.com",
            knownPackageNames = listOf("com.linkedin.android"),
            monogram = "IN",
            brandColorHex = 0xFF0A66C2,
            defaultDesktopUa = false,
            helperHint = "Isolated professional LinkedIn login."
        ),
        DualAppPreset(
            key = "reddit",
            name = "Reddit",
            category = "Social",
            url = "https://www.reddit.com",
            knownPackageNames = listOf("com.reddit.frontpage"),
            monogram = "RD",
            brandColorHex = 0xFFFF4500,
            defaultDesktopUa = false,
            helperHint = "Alternate Reddit account with zero cross-session tracking."
        )
    )

    val quickStartPresets: List<DualAppPreset> = presets.take(6)

    fun findByKey(key: String?): DualAppPreset? =
        if (key == null) null else presets.firstOrNull { it.key == key }

    fun findByPackageName(packageName: String?): DualAppPreset? =
        if (packageName.isNullOrBlank()) null
        else presets.firstOrNull { it.knownPackageNames.contains(packageName) }
}
