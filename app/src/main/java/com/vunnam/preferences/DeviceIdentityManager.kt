package com.vunnam.preferences

import kotlin.random.Random

object DeviceIdentityManager {

    val FUNNY_NICKNAMES = listOf(
        "Dancing Potato 🥔",
        "Wi-Fi Bandit 🏴‍☠️",
        "Captain Lag 🚀",
        "Bluetooth Biryani 🍛",
        "Pocket Dragon 🐉",
        "Chotu Rocket 🚀",
        "File Ninja 🥷",
        "Chai Addict ☕",
        "The Invisible Man 👻",
        "Data Don 🕶️",
        "Angry Penguin 🐧",
        "Transfer Maharaja 👑",
        "Turbo Tortoise 🐢",
        "Meme Machine 😂",
        "Quantum Samosa 🥟",
        "Laser Llama 🦙",
        "Disco Doughnut 🍩",
        "Byte Biker 🏍️",
        "Sleepy Koala 🐨",
        "Cosmic Dosa 🥞",
        "Midnight Coder 💻",
        "Speedy Sloth 🦥",
        "Neon Flamingo 🦩",
        "Pixel Pirate 🏴‍☠️",
        "Sassy Cactus 🌵",
        "Butter Chicken 🍗",
        "Velvet Waffle 🧇",
        "Electric Eel ⚡",
        "Giga Chad 🗿",
        "Spicy Momos 🥟",
        "Zen Master 🧘",
        "Hyper Hamster 🐹",
        "Rocket Raccoon 🦝",
        "Galactic Panda 🐼",
        "Golden Goose 🪿",
        "Flying Vada Pav 🍔",
        "Couch Commander 🛋️",
        "Radioactive Radish 🥕",
        "Choco Lava 🍫",
        "Zero Latency 🎯",
        "Sir Ping-a-lot 🏓",
        "Taco Tornado 🌮",
        "Bamboozle Bot 🤖",
        "Boba Bandit 🧋",
        "Crazy Croissant 🥐",
        "Vibe Voyager 🛸",
        "Pancake Pilot 🥞",
        "Sneaky Squirrel 🐿️",
        "Jumping Jellyfish 🪼",
        "Breezy Badger 🦡",
        "Hotdog Hero 🌭",
        "Cheesy Nacho 🧀",
        "Noodle Knight 🍜",
        "Super Snail 🐌",
        "Blinky Bill 🦆",
        "Chirpy Cheetah 🐆",
        "Magic Mango 🥭",
        "Bouncy Beagle 🐶",
        "Mystic Muffin 🧁",
        "Sonic Hedgehog 🦔",
        "Glitch Gremlin 👾",
        "Cloud Cruiser ☁️",
        "Funky Monkey 🐒",
        "Groovy Gecko 🦎",
        "Party Parrot 🦜",
        "Dapper Dinosaur 🦖",
        "Snooze Button ⏰",
        "Snack Wizard 🪄",
        "Espresso Express ☕",
        "Popcorn Pooper 🍿",
        "Wobbly Walrus 🦭",
        "Chilly Chili 🌶️",
        "Starlight Sloth 🌟",
        "Velociraptor VIP 🦖",
        "Thunder Tiger 🐅",
        "Giggle Goose 🪿",
        "Silly Salmon 🐟",
        "Dizzy Duck 🦆",
        "Fizzy Soda 🥤",
        "Turbo Taco 🌮",
        "Crispy Nugget 🍗",
        "Phantom Pickle 🥒",
        "Waffle Warrior 🧇",
        "Matrix Mule 🐴",
        "Galaxy Guppy 🐠",
        "Curry Comet ☄️",
        "Lofi Leopard 🐆",
        "Cookie Crumb 🍪",
        "Astral Alpaca 🦙",
        "Boba Bear 🐻",
        "Atomic Avocado 🥑",
        "Panda Express 🐼",
        "Turbo Toucan 🦜",
        "Samosa Samurai 🥟",
        "Ninja Nacho 🥷",
        "Flash Flamingo 🦩",
        "Bacon Boss 🥓",
        "Vortex Viper 🐍",
        "Space Sparrow 🐦",
        "Happy Hippo 🦛",
        "Kangaroo Kick 🦘",
        "Peanut Butter 🥜",
        "Gummy Bear 🍬",
        "Sparky Otter 🦦",
        "Blaze Bunny 🐰",
        "Zesty Lemon 🍋"
    )

    data class AvatarOption(
        val id: Int,
        val emoji: String,
        val label: String,
        val colorHex: Long
    )

    val AVATAR_OPTIONS = listOf(
        AvatarOption(0, "🚀", "Rocket", 0xFF00D2FF),
        AvatarOption(1, "🥔", "Potato", 0xFFF59E0B),
        AvatarOption(2, "🥷", "Ninja", 0xFF6366F1),
        AvatarOption(3, "🐉", "Dragon", 0xFFEF4444),
        AvatarOption(4, "☕", "Chai", 0xFFD97706),
        AvatarOption(5, "👑", "Maharaja", 0xFFFBBF24),
        AvatarOption(6, "👻", "Ghost", 0xFFA855F7),
        AvatarOption(7, "🐧", "Penguin", 0xFF3B82F6),
        AvatarOption(8, "🐢", "Turbo", 0xFF10B981),
        AvatarOption(9, "😂", "Meme", 0xFFEC4899),
        AvatarOption(10, "🌮", "Taco", 0xFFF97316),
        AvatarOption(11, "🦙", "Llama", 0xFF8B5CF6),
        AvatarOption(12, "🐼", "Panda", 0xFF06B6D4),
        AvatarOption(13, "⚡", "Volt", 0xFFEAB308),
        AvatarOption(14, "🛸", "UFO", 0xFF14B8A6),
        AvatarOption(15, "🦄", "Unicorn", 0xFFF43F5E)
    )

    fun getRandomNickname(): String {
        return FUNNY_NICKNAMES[Random.nextInt(FUNNY_NICKNAMES.size)]
    }

    fun getRandomAvatar(): AvatarOption {
        return AVATAR_OPTIONS[Random.nextInt(AVATAR_OPTIONS.size)]
    }

    fun getAvatarById(id: Int): AvatarOption {
        return AVATAR_OPTIONS.firstOrNull { it.id == id } ?: AVATAR_OPTIONS.first()
    }
}
