package com.fjordflow.data.seed

import com.fjordflow.data.db.entity.RoadmapNodeEntity

val frenchRoadmapNodes = listOf(
    RoadmapNodeEntity(1,  "Alphabet & Sounds",      "Master French phonetics and pronunciation",          "fr", 0,  "Foundations", isUnlocked = true),
    RoadmapNodeEntity(2,  "Greetings & Basics",     "Bonjour, merci, s'il vous plaît, au revoir",         "fr", 1,  "Foundations", isUnlocked = true),
    RoadmapNodeEntity(3,  "Numbers & Time",          "Counting and telling time in French",                "fr", 2,  "Foundations"),
    RoadmapNodeEntity(4,  "Gendered Nouns",          "Understanding le, la, un, une and noun gender",      "fr", 3,  "Grammar"),
    RoadmapNodeEntity(5,  "Articles",                "Definite, indefinite, and partitive articles",       "fr", 4,  "Grammar"),
    RoadmapNodeEntity(6,  "être & avoir",            "Master the two essential verbs: to be and to have",  "fr", 5,  "Grammar"),
    RoadmapNodeEntity(7,  "Regular -er Verbs",       "Parler, manger, aimer — the most common pattern",   "fr", 6,  "Grammar"),
    RoadmapNodeEntity(8,  "Regular -ir & -re Verbs", "Finir, choisir, vendre, répondre",                  "fr", 7,  "Grammar"),
    RoadmapNodeEntity(9,  "Adjective Agreement",     "Gender and number agreement for adjectives",         "fr", 8,  "Grammar"),
    RoadmapNodeEntity(10, "Negation",                "ne...pas, ne...jamais, ne...rien",                   "fr", 9,  "Grammar"),
    RoadmapNodeEntity(11, "Questions",               "Inversion, est-ce que, and question words",          "fr", 10, "Grammar"),
    RoadmapNodeEntity(12, "Passé Composé",           "Past actions with avoir and être as auxiliaries",    "fr", 11, "Tenses"),
    RoadmapNodeEntity(13, "Imparfait",               "Describing ongoing past states and habits",          "fr", 12, "Tenses"),
    RoadmapNodeEntity(14, "Futur Simple",            "Conjugating verbs in the simple future",             "fr", 13, "Tenses"),
    RoadmapNodeEntity(15, "Futur Proche",            "Aller + infinitive for near future",                 "fr", 14, "Tenses"),
    RoadmapNodeEntity(16, "Conditionnel Présent",    "Would constructions and polite requests",            "fr", 15, "Advanced"),
    RoadmapNodeEntity(17, "Subjonctif Présent",      "Subjunctive mood after il faut que, vouloir que",   "fr", 16, "Advanced"),
    RoadmapNodeEntity(18, "Relative Pronouns",       "qui, que, dont, où",                                "fr", 17, "Advanced"),
    RoadmapNodeEntity(19, "Passive Voice",           "Être + past participle constructions",               "fr", 18, "Advanced"),
    RoadmapNodeEntity(20, "Literary Tenses",         "Passé Simple, Subjonctif Imparfait",                "fr", 19, "Advanced"),
)
