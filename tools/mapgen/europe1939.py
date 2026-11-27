"""Theatre definition for the map of Europe and North Africa, September 1939.

Geography (land, sea, lakes, rivers, mountain ranges) comes from Natural Earth; everything here is
our own design data: which territories exist, the 1939 borders that differ from today's, cities,
straits and hand-made fixes. Coordinates are (longitude, latitude) in degrees. See
docs/mapa-europa-1939.md.
"""

NATURAL_EARTH_VERSION = "5.1.2"
MAP_ID = "europe-1939"
MAP_NAME = "Europa y norte de África, 1939"

# ------------------------------------------------------------------ territories
# (id, nation, kind, name). Homelands use the nation code; colonies and possessions
# "<NATION>-<code>".
TERRITORIES = [
    ("DEU", "DEU", "homeland", "Alemania"),
    ("ITA", "ITA", "homeland", "Italia"),
    ("FIN", "FIN", "homeland", "Finlandia"),
    ("ROU", "ROU", "homeland", "Rumanía"),
    ("HUN", "HUN", "homeland", "Hungría"),
    ("BGR", "BGR", "homeland", "Bulgaria"),
    ("GBR", "GBR", "homeland", "Reino Unido"),
    ("FRA", "FRA", "homeland", "Francia"),
    ("NOR", "NOR", "homeland", "Noruega"),
    ("NLD", "NLD", "homeland", "Países Bajos"),
    ("BEL", "BEL", "homeland", "Bélgica"),
    ("GRC", "GRC", "homeland", "Grecia"),
    ("DNK", "DNK", "homeland", "Dinamarca"),
    ("SUN", "SUN", "homeland", "Unión Soviética"),
    ("POL", "POL", "homeland", "Polonia"),
    ("YUG", "YUG", "homeland", "Yugoslavia"),
    ("ESP", "ESP", "homeland", "España"),
    ("TUR", "TUR", "homeland", "Turquía"),
    ("SWE", "SWE", "homeland", "Suecia"),
    ("CHE", "CHE", "homeland", "Suiza"),
    ("IRL", "IRL", "homeland", "Irlanda"),
    ("PRT", "PRT", "homeland", "Portugal"),
    ("DEU-SVK", "DEU", "possession", "Eslovaquia"),
    ("ITA-LBY", "ITA", "colony", "Libia"),
    ("ITA-ALB", "ITA", "possession", "Albania"),
    ("ITA-DOD", "ITA", "possession", "Dodecaneso"),
    ("GBR-EGY", "GBR", "possession", "Egipto"),
    ("GBR-PSE", "GBR", "possession", "Palestina"),
    ("GBR-JOR", "GBR", "possession", "Transjordania"),
    ("GBR-CYP", "GBR", "colony", "Chipre"),
    ("GBR-MLT", "GBR", "colony", "Malta"),
    ("GBR-GIB", "GBR", "colony", "Gibraltar"),
    ("FRA-DZA", "FRA", "colony", "Argelia"),
    ("FRA-TUN", "FRA", "possession", "Túnez"),
    ("FRA-MAR", "FRA", "possession", "Marruecos francés"),
    ("FRA-SYR", "FRA", "possession", "Siria y Líbano"),
    ("ESP-MAR", "ESP", "possession", "Marruecos español"),
    ("DNK-ISL", "DNK", "possession", "Islandia"),
    ("SUN-BAL", "SUN", "possession", "Países Bálticos"),
]

# Natural Earth country (ADM0_A3) -> territory. None: off the board.
COUNTRY_TO_TERRITORY = {
    "DEU": "DEU", "AUT": "DEU", "CZE": "DEU", "SVK": "DEU-SVK",
    "POL": "POL", "LTU": "SUN-BAL", "LVA": "SUN-BAL", "EST": "SUN-BAL",
    "RUS": "SUN", "BLR": "SUN", "UKR": "SUN", "GEO": "SUN", "ARM": "SUN", "AZE": "SUN", "KAZ": "SUN",
    "TKM": "SUN", "MDA": "ROU",
    "ITA": "ITA", "SMR": "ITA", "VAT": "ITA", "ALB": "ITA-ALB", "LBY": "ITA-LBY",
    "FRA": "FRA", "MCO": "FRA", "DZA": "FRA-DZA", "TUN": "FRA-TUN", "MAR": "FRA-MAR",
    "SYR": "FRA-SYR", "LBN": "FRA-SYR",
    "GBR": "GBR", "IMN": "GBR", "GGY": "GBR", "JEY": "GBR", "MLT": "GBR-MLT",
    "CYP": "GBR-CYP", "CYN": "GBR-CYP", "EGY": "GBR-EGY", "ISR": "GBR-PSE", "PSX": "GBR-PSE",
    "JOR": "GBR-JOR",
    "IRL": "IRL", "NLD": "NLD", "BEL": "BEL", "LUX": "BEL", "DNK": "DNK", "ISL": "DNK-ISL",
    "NOR": "NOR", "SWE": "SWE", "FIN": "FIN", "ALD": "FIN",
    "CHE": "CHE", "LIE": "CHE", "ESP": "ESP", "AND": "ESP", "PRT": "PRT",
    "HUN": "HUN", "ROU": "ROU", "BGR": "BGR", "GRC": "GRC", "TUR": "TUR",
    "SRB": "YUG", "HRV": "YUG", "BIH": "YUG", "MNE": "YUG", "MKD": "YUG", "SVN": "YUG", "KOS": "YUG",
    "IRQ": None, "IRN": None, "SAU": None, "KWT": None, "BHR": None, "QAT": None,
    "SAH": None, "MRT": None, "FRO": None, "GRL": None,
}

# 1939 borders that differ from today's, as rough polygons. The first one that contains a point
# wins, before COUNTRY_TO_TERRITORY. At 110 km per hex a few vertices per border are enough.
BORDERS_1939 = [
    # Pomerania, Brandenburg east of the Oder and Silesia (now Poland), up to the 1939 border.
    ("DEU", [(13.5, 55.0), (17.7, 54.85), (17.45, 54.4), (17.25, 53.95), (16.95, 53.6), (17.05, 53.3),
             (16.6, 53.1), (16.1, 52.8), (15.85, 52.45), (16.0, 52.2), (16.3, 51.95), (16.55, 51.7),
             (17.2, 51.55), (17.6, 51.3), (17.9, 51.15), (18.2, 51.05), (18.45, 50.75), (18.65, 50.5),
             (18.85, 50.35), (18.7, 50.2), (18.4, 50.0), (18.0, 49.95), (13.5, 49.95)]),
    # Danzig, East Prussia and Memel.
    ("DEU", [(18.45, 54.45), (18.45, 54.25), (18.85, 54.0), (18.85, 53.75), (19.3, 53.45),
             (20.2, 53.3), (21.0, 53.5), (21.8, 53.55), (22.4, 53.75), (22.75, 54.35), (22.85, 54.85),
             (22.2, 55.05), (22.15, 55.2), (21.65, 55.75), (21.05, 56.05), (20.4, 55.6), (19.5, 54.9),
             (18.45, 54.9)]),
    # Southern Slovakia (Vienna award, 1938) and Carpatho-Ukraine (March 1939) went to Hungary.
    ("HUN", [(17.8, 47.75), (18.6, 48.05), (19.5, 48.2), (20.4, 48.35), (21.0, 48.5), (21.25, 48.75),
             (21.8, 48.6), (22.3, 48.95), (22.6, 49.1), (23.5, 48.95), (24.6, 48.0), (23.0, 47.8),
             (17.8, 47.6)]),
    # Bessarabia and Northern Bukovina.
    ("ROU", [(24.8, 47.7), (25.3, 48.3), (26.2, 48.6), (27.5, 48.5), (28.3, 48.05), (29.2, 47.45),
             (29.6, 46.8), (30.2, 46.45), (30.6, 46.0), (29.8, 45.15), (28.0, 45.2), (27.0, 46.5),
             (25.5, 47.5)]),
    # Southern Dobruja.
    ("ROU", [(26.4, 44.15), (27.4, 44.15), (28.7, 43.8), (28.7, 43.35), (28.0, 43.3), (27.0, 43.7),
             (26.4, 43.95)]),
    # Poland, 1921-1939 borders (west and north handled by the German polygons above).
    ("POL", [(15.5, 54.9), (18.6, 54.9), (18.5, 54.3), (19.0, 53.6), (20.2, 53.25), (21.0, 53.45),
             (22.4, 53.7), (22.8, 54.35), (23.5, 54.25), (24.2, 54.2), (24.6, 54.5), (24.9, 54.85),
             (25.6, 55.2), (26.6, 55.7), (27.9, 55.75), (27.9, 54.6), (27.3, 54.1), (26.9, 53.6),
             (27.3, 53.0), (27.7, 52.3), (27.9, 51.6), (27.4, 51.0), (27.2, 50.55), (26.8, 50.2),
             (26.15, 49.6), (26.1, 49.0), (26.3, 48.6), (25.6, 48.45), (24.6, 47.95), (23.5, 48.9),
             (22.8, 49.1), (22.0, 49.2), (21.0, 49.4), (20.0, 49.2), (19.2, 49.4), (18.85, 49.5),
             (18.6, 49.9), (17.5, 50.2), (17.0, 51.5), (15.5, 53.0)]),
    # Finnish Karelia: the Karelian Isthmus and the north shore of Lake Ladoga.
    ("FIN", [(27.7, 60.45), (29.3, 60.15), (30.0, 60.25), (30.6, 60.6), (31.0, 61.0), (31.8, 61.4),
             (32.6, 62.0), (31.6, 62.9), (30.0, 63.2), (27.7, 62.0)]),
    # Petsamo.
    ("FIN", [(28.4, 69.05), (29.3, 69.95), (31.6, 69.85), (31.4, 69.2), (29.5, 68.9)]),
    # Istria, Fiume and the Julian March went to Italy after 1918.
    ("ITA", [(13.5, 46.55), (13.85, 46.4), (14.0, 46.0), (14.3, 45.75), (14.45, 45.35), (14.2, 44.85),
             (13.6, 44.75), (13.0, 45.6)]),
    # Dodecanese.
    ("ITA-DOD", [(26.3, 37.45), (27.3, 37.45), (28.5, 36.6), (28.4, 35.3), (26.8, 35.3), (26.3, 36.4)]),
    # Spanish protectorate in northern Morocco (with Tangier).
    ("ESP-MAR", [(-6.4, 36.0), (-5.3, 36.0), (-4.0, 35.35), (-2.85, 35.35), (-2.85, 35.0), (-3.4, 34.85),
                 (-4.4, 34.9), (-5.3, 34.75), (-6.3, 34.95)]),
]

# Land south of these latitudes leaves the board (Sahara).
SOUTH_CUT = {"FRA-MAR": 31.0, "FRA-DZA": 33.0, "FRA-TUN": 32.0, "ITA-LBY": 29.0, "GBR-EGY": 28.5,
             "GBR-JOR": 29.0}

# ------------------------------------------------------------------ physical geography
LAKES = {
    "Lake Ladoga", "Lake Onega", "Vänern", "Vättern", "Lake Peipus", "Lake Pskov", "Lake Il'Men'",
    "Lake Saimaa", "Päijänne", "Lake Balaton", "Lake Geneva", "Bodensee", "Lake Beloye", "Lake Van",
    "Lake Tuz", "Lake Sevan", "Inarijärvi", "Oulujärvi", "Lake Topozero", "Lake Skadar", "Imandra",
}

# Natural Earth 1:10m physical regions (featurecla Range/mtn) counted as mountains.
MOUNTAIN_RANGES = {
    "ALPS", "APPENNINI", "PYRENEES", "CARPATHIAN MOUNTAINS", "Transylvanian Alps", "Tatra Mts.",
    "Dinaric Alps", "Balkan Mts.", "Pindus Mts.", "CAUCASUS MTS.", "Lesser Caucasus", "PONTIC MOUNTAINS",
    "Taurus Mts.", "KJØLEN MOUNTAINS", "Hardanger-vidda", "HAUT ATLAS", "Moyen Atlas", "ATLAS SAHARIEN",
    "Atlas Tellien", "Er Rif", "Cord. Cantábrica", "S. Nevada", "Lebanon Mts.", "Grampian Mts.",
}
# Ranges that Natural Earth does not outline.
EXTRA_MOUNTAINS = [
    # Rhodope.
    [(23.4, 42.0), (25.8, 42.0), (25.8, 41.35), (23.4, 41.35)],
]
MOUNTAIN_SHARE = 0.5  # share of the hex land covered by ranges

# Rivers drawn on hex sides (Natural Earth 1:10m, name_en).
RIVERS = [
    "Rhine", "Rhein", "Waal", "Danube", "Elbe", "Oder", "Vistula", "Dnieper", "Don", "Volga", "Dniester",
    "Seine", "Loire", "Rhône", "Po", "Nemunas", "Zapadnaya Dvina", "Ebro", "Tagus", "Tajo", "Duero",
    "Bug", "Southern Bug", "Prut", "Tisa", "Sava", "Desna", "Donets", "Oka", "Maas", "Garonne",
    "Svir", "Neva", "Volkhov", "Kuban", "Nile", "Damietta Branch", "Rosetta Branch",
]
EXTRA_RIVER_SIDES = []
REMOVED_RIVER_SIDES = []

# Islands between these sizes keep at least one land hex, plus every hex they cover this much.
ISLAND_MIN_KM2 = 1000
ISLAND_MAX_KM2 = 50000
ISLAND_HEX_SHARE = 0.35

# ------------------------------------------------------------------ fixes by point
# (label, lon, lat, values): the hex that contains the point gets these values (None removes it).
FORCED_HEXES = [
    ("Gibraltar", -5.35, 36.14, {"terrain": "land", "territory": "GBR-GIB"}),
    ("Malta", 14.45, 35.9, {"terrain": "land", "territory": "GBR-MLT"}),
    ("Rodas", 28.0, 36.2, {"terrain": "land", "territory": "ITA-DOD"}),
]
# Fixes by axial coordinate, for retouches made after looking at the map: {(q, r): values}.
HEX_OVERRIDES = {
    # Spanish Morocco: the Rif coast, south of the Gibraltar strait hex.
    (-16, 39): {"terrain": "land", "territory": "ESP-MAR"},
    (-15, 39): {"terrain": "land", "territory": "ESP-MAR"},
    # Edirne and Turkish Thrace.
    (11, 34): {"terrain": "land", "territory": "TUR"},
    # Kerch peninsula, west shore of the Kerch strait.
    (20, 28): {"terrain": "land", "territory": "SUN"},
    # Haifa and Galilee.
    (15, 43): {"terrain": "land", "territory": "GBR-PSE"},
    # Flanders coast and Antwerp.
    (2, 22): {"terrain": "land", "territory": "BEL"},
    # Kolguyev island, in the corner of the board: out of play.
    (33, 0): None,
}

# ------------------------------------------------------------------ straits (decision D4)
# (name, sea hex, shore A, shore B) in axial (q, r), chosen by looking at the generated grid: the
# sea hex is forced to sea and both shores must be land next to it.
STRAITS = [
    ("Gibraltar", (-16, 38), (-16, 37), (-16, 39)),
    ("Dardanelos", (11, 35), (12, 34), (11, 36)),
    ("Bósforo", (13, 34), (12, 34), (13, 35)),
    ("Mesina", (1, 38), (2, 37), (0, 39)),
    ("Kerch", (21, 28), (20, 28), (22, 28)),
    ("Dover", (1, 22), (0, 22), (1, 23)),
    ("Øresund", (10, 18), (9, 18), (11, 17)),
    ("Bonifacio", (-2, 34), (-1, 33), (-3, 35)),
]

# ------------------------------------------------------------------ cities
# Cities whose capture counts for victory (the 36 objective cities of the rules); the map only
# makes sure they exist, the rule set decides how they are used.
OBJECTIVE_CITIES = [
    "Londres", "París", "Moscú", "Berlín", "Roma", "Stalingrado", "Leningrado", "Suez", "Trípoli",
    "Helsinki", "Gibraltar", "Kiev", "La Valeta", "Copenhague", "Oslo", "Atenas", "Sofía", "Bucarest",
    "Budapest", "Belgrado", "Varsovia", "Bruselas", "Róterdam", "Argel", "Casablanca", "Madrid",
    "Barcelona", "Dublín", "Ankara", "Estambul", "Berna", "Estocolmo", "Amán", "Túnez", "Járkov", "Kursk",
]

NC, TC, C = "national_capital", "territory_capital", "city"
OPT = {"optional": True}
# (name, lon, lat, kind, territory[, options]). Capitals first, then objectives, then the rest:
# when two fall in the same hex the later one moves to the nearest free hex of its territory.
CITIES = [
    # capitals
    ("Berlín", 13.40, 52.52, NC, "DEU"),
    ("Roma", 12.50, 41.90, NC, "ITA"),
    ("Helsinki", 24.94, 60.17, NC, "FIN"),
    ("Bucarest", 26.10, 44.43, NC, "ROU"),
    ("Budapest", 19.04, 47.50, NC, "HUN"),
    ("Sofía", 23.32, 42.70, NC, "BGR"),
    ("Londres", -0.13, 51.51, NC, "GBR"),
    ("París", 2.35, 48.86, NC, "FRA"),
    ("Oslo", 10.75, 59.91, NC, "NOR"),
    ("Ámsterdam", 4.90, 52.37, NC, "NLD"),
    ("Bruselas", 4.35, 50.85, NC, "BEL"),
    ("Atenas", 23.73, 37.98, NC, "GRC"),
    ("Copenhague", 12.57, 55.68, NC, "DNK"),
    ("Moscú", 37.62, 55.76, NC, "SUN"),
    ("Varsovia", 21.01, 52.23, NC, "POL"),
    ("Belgrado", 20.46, 44.82, NC, "YUG"),
    ("Madrid", -3.70, 40.42, NC, "ESP"),
    ("Ankara", 32.86, 39.93, NC, "TUR"),
    ("Estocolmo", 18.07, 59.33, NC, "SWE"),
    ("Berna", 7.45, 46.95, NC, "CHE"),
    ("Dublín", -6.26, 53.35, NC, "IRL"),
    ("Lisboa", -9.14, 38.72, NC, "PRT"),
    ("Bratislava", 17.11, 48.15, TC, "DEU-SVK"),
    ("Trípoli", 13.19, 32.89, TC, "ITA-LBY"),
    ("Tirana", 19.82, 41.33, TC, "ITA-ALB"),
    ("Rodas", 28.22, 36.44, TC, "ITA-DOD"),
    ("El Cairo", 31.24, 30.04, TC, "GBR-EGY"),
    ("Jerusalén", 35.22, 31.78, TC, "GBR-PSE"),
    ("Amán", 35.93, 31.95, TC, "GBR-JOR"),
    ("Nicosia", 33.36, 35.17, TC, "GBR-CYP"),
    ("La Valeta", 14.51, 35.90, TC, "GBR-MLT"),
    ("Gibraltar", -5.35, 36.14, TC, "GBR-GIB"),
    ("Argel", 3.06, 36.75, TC, "FRA-DZA"),
    ("Túnez", 10.18, 36.81, TC, "FRA-TUN"),
    ("Rabat", -6.84, 34.02, TC, "FRA-MAR"),
    ("Damasco", 36.29, 33.51, TC, "FRA-SYR"),
    ("Tetuán", -5.37, 35.57, TC, "ESP-MAR"),
    ("Reikiavik", -21.94, 64.15, TC, "DNK-ISL"),
    ("Riga", 24.11, 56.95, TC, "SUN-BAL"),
    # objectives that are not capitals
    ("Stalingrado", 44.52, 48.71, C, "SUN"),
    ("Leningrado", 30.32, 59.94, C, "SUN"),
    ("Kiev", 30.52, 50.45, C, "SUN"),
    ("Járkov", 36.23, 49.99, C, "SUN"),
    ("Kursk", 36.19, 51.73, C, "SUN"),
    ("Suez", 32.55, 29.97, C, "GBR-EGY"),
    ("Róterdam", 4.48, 51.92, C, "NLD"),
    ("Casablanca", -7.59, 33.57, C, "FRA-MAR"),
    ("Barcelona", 2.17, 41.39, C, "ESP"),
    ("Estambul", 28.98, 41.01, C, "TUR"),
    # Germany (with Austria and Bohemia-Moravia)
    ("Hamburgo", 9.99, 53.55, C, "DEU"),
    ("Múnich", 11.58, 48.14, C, "DEU"),
    ("Colonia", 6.96, 50.94, C, "DEU"),
    ("Essen", 7.01, 51.46, C, "DEU"),
    ("Fráncfort", 8.68, 50.11, C, "DEU"),
    ("Stuttgart", 9.18, 48.78, C, "DEU"),
    ("Leipzig", 12.37, 51.34, C, "DEU"),
    ("Dresde", 13.74, 51.05, C, "DEU"),
    ("Breslau", 17.04, 51.11, C, "DEU"),
    ("Königsberg", 20.51, 54.71, C, "DEU"),
    ("Stettin", 14.55, 53.43, C, "DEU"),
    ("Hannover", 9.73, 52.37, C, "DEU"),
    ("Núremberg", 11.08, 49.45, C, "DEU"),
    ("Bremen", 8.80, 53.08, C, "DEU"),
    ("Kiel", 10.13, 54.32, C, "DEU"),
    ("Danzig", 18.65, 54.35, C, "DEU"),
    ("Viena", 16.37, 48.21, C, "DEU"),
    ("Graz", 15.44, 47.07, C, "DEU"),
    ("Praga", 14.42, 50.08, C, "DEU"),
    ("Brno", 16.61, 49.20, C, "DEU"),
    # Italy
    ("Milán", 9.19, 45.46, C, "ITA"),
    ("Turín", 7.69, 45.07, C, "ITA"),
    ("Génova", 8.93, 44.41, C, "ITA"),
    ("Venecia", 12.33, 45.44, C, "ITA"),
    ("Bolonia", 11.34, 44.49, C, "ITA"),
    ("Florencia", 11.26, 43.77, C, "ITA"),
    ("Nápoles", 14.27, 40.85, C, "ITA"),
    ("Bari", 16.87, 41.12, C, "ITA"),
    ("Palermo", 13.36, 38.12, C, "ITA"),
    ("Catania", 15.09, 37.50, C, "ITA"),
    ("Cagliari", 9.11, 39.22, C, "ITA"),
    ("Trieste", 13.78, 45.65, C, "ITA"),
    ("Ancona", 13.52, 43.62, C, "ITA"),
    ("Bengasi", 20.07, 32.12, C, "ITA-LBY"),
    ("Tobruk", 23.96, 32.08, C, "ITA-LBY"),
    ("Misurata", 15.09, 32.38, C, "ITA-LBY"),
    ("Derna", 22.64, 32.77, C, "ITA-LBY"),
    ("Sirte", 16.59, 31.21, C, "ITA-LBY"),
    # Finland
    ("Turku", 22.27, 60.45, C, "FIN"),
    ("Tampere", 23.76, 61.50, C, "FIN"),
    ("Viipuri", 28.75, 60.71, C, "FIN"),
    ("Oulu", 25.47, 65.01, C, "FIN"),
    ("Rovaniemi", 25.73, 66.50, C, "FIN"),
    ("Petsamo", 31.15, 69.55, C, "FIN"),
    ("Vaasa", 21.62, 63.10, C, "FIN"),
    ("Kuopio", 27.68, 62.89, C, "FIN"),
    # Romania
    ("Ploiești", 26.02, 44.94, C, "ROU"),
    ("Constanza", 28.63, 44.17, C, "ROU"),
    ("Iași", 27.59, 47.16, C, "ROU"),
    ("Cluj", 23.59, 46.77, C, "ROU"),
    ("Timișoara", 21.23, 45.75, C, "ROU"),
    ("Chișinău", 28.86, 47.01, C, "ROU"),
    ("Cernăuți", 25.94, 48.29, C, "ROU"),
    ("Galați", 28.05, 45.44, C, "ROU"),
    ("Brașov", 25.60, 45.65, C, "ROU"),
    ("Craiova", 23.80, 44.32, C, "ROU"),
    # Hungary
    ("Debrecen", 21.63, 47.53, C, "HUN"),
    ("Szeged", 20.15, 46.25, C, "HUN"),
    ("Pécs", 18.23, 46.07, C, "HUN"),
    ("Győr", 17.63, 47.69, C, "HUN"),
    ("Kassa", 21.26, 48.72, C, "HUN"),
    # Bulgaria
    ("Plovdiv", 24.75, 42.14, C, "BGR"),
    ("Varna", 27.91, 43.21, C, "BGR"),
    ("Burgas", 27.47, 42.50, C, "BGR"),
    ("Ruse", 25.97, 43.85, C, "BGR"),
    # United Kingdom
    ("Birmingham", -1.90, 52.49, C, "GBR"),
    ("Mánchester", -2.24, 53.48, C, "GBR"),
    ("Liverpool", -2.98, 53.41, C, "GBR"),
    ("Newcastle", -1.61, 54.97, C, "GBR"),
    ("Glasgow", -4.25, 55.86, C, "GBR"),
    ("Edimburgo", -3.19, 55.95, C, "GBR"),
    ("Belfast", -5.93, 54.60, C, "GBR"),
    ("Cardiff", -3.18, 51.48, C, "GBR"),
    ("Southampton", -1.40, 50.90, C, "GBR"),
    ("Norwich", 1.30, 52.63, C, "GBR"),
    ("Alejandría", 29.92, 31.20, C, "GBR-EGY"),
    ("Port Said", 32.30, 31.26, C, "GBR-EGY"),
    ("Mersa Matruh", 27.24, 31.35, C, "GBR-EGY"),
    ("Haifa", 34.99, 32.79, C, "GBR-PSE"),
    # France
    ("Lyon", 4.84, 45.76, C, "FRA"),
    ("Marsella", 5.37, 43.30, C, "FRA"),
    ("Tolón", 5.93, 43.12, C, "FRA"),
    ("Burdeos", -0.58, 44.84, C, "FRA"),
    ("Toulouse", 1.44, 43.60, C, "FRA"),
    ("Nantes", -1.55, 47.22, C, "FRA"),
    ("Brest", -4.49, 48.39, C, "FRA"),
    ("Lille", 3.06, 50.63, C, "FRA"),
    ("Estrasburgo", 7.75, 48.57, C, "FRA"),
    ("Metz", 6.18, 49.12, C, "FRA"),
    ("Reims", 4.03, 49.26, C, "FRA"),
    ("Dijon", 5.04, 47.32, C, "FRA"),
    ("Le Havre", 0.11, 49.49, C, "FRA"),
    ("Niza", 7.26, 43.70, C, "FRA"),
    ("Montpellier", 3.88, 43.61, C, "FRA"),
    ("Limoges", 1.26, 45.83, C, "FRA"),
    ("Ajaccio", 8.74, 41.92, C, "FRA"),
    ("Orán", -0.64, 35.70, C, "FRA-DZA"),
    ("Constantina", 6.61, 36.37, C, "FRA-DZA"),
    ("Bona", 7.77, 36.90, C, "FRA-DZA"),
    ("Biskra", 5.73, 34.85, C, "FRA-DZA"),
    ("Sfax", 10.76, 34.74, C, "FRA-TUN"),
    ("Gabès", 10.10, 33.88, C, "FRA-TUN"),
    ("Fez", -5.00, 34.03, C, "FRA-MAR"),
    ("Marrakech", -7.99, 31.63, C, "FRA-MAR"),
    ("Uchda", -1.91, 34.68, C, "FRA-MAR"),
    ("Beirut", 35.50, 33.89, C, "FRA-SYR"),
    ("Alepo", 37.16, 36.20, C, "FRA-SYR"),
    ("Homs", 36.72, 34.73, C, "FRA-SYR"),
    ("Latakia", 35.78, 35.52, C, "FRA-SYR"),
    ("Deir ez-Zor", 40.14, 35.33, C, "FRA-SYR"),
    # Norway
    ("Bergen", 5.32, 60.39, C, "NOR"),
    ("Stavanger", 5.73, 58.97, C, "NOR"),
    ("Trondheim", 10.40, 63.43, C, "NOR"),
    ("Narvik", 17.43, 68.44, C, "NOR"),
    ("Tromsø", 18.96, 69.65, C, "NOR"),
    ("Kristiansand", 8.00, 58.15, C, "NOR"),
    ("Bodø", 14.40, 67.28, C, "NOR"),
    ("Kirkenes", 30.05, 69.73, C, "NOR"),
    # Low countries
    ("Amberes", 4.40, 51.22, C, "BEL"),
    ("Lieja", 5.57, 50.63, C, "BEL"),
    ("Luxemburgo", 6.13, 49.61, C, "BEL", OPT),
    # Greece
    ("Salónica", 22.94, 40.64, C, "GRC"),
    ("Patras", 21.73, 38.25, C, "GRC"),
    ("Larisa", 22.42, 39.64, C, "GRC"),
    ("Ioánina", 20.85, 39.66, C, "GRC"),
    ("Heraclión", 25.13, 35.34, C, "GRC"),
    ("Kavala", 24.41, 40.94, C, "GRC"),
    # Denmark
    ("Aarhus", 10.20, 56.16, C, "DNK"),
    ("Odense", 10.39, 55.40, C, "DNK"),
    ("Esbjerg", 8.45, 55.47, C, "DNK"),
    ("Akureyri", -18.09, 65.68, C, "DNK-ISL"),
    # Soviet Union
    ("Minsk", 27.56, 53.90, C, "SUN"),
    ("Odesa", 30.73, 46.48, C, "SUN"),
    ("Sebastopol", 33.52, 44.62, C, "SUN"),
    ("Rostov", 39.72, 47.23, C, "SUN"),
    ("Smolensk", 32.05, 54.78, C, "SUN"),
    ("Gorki", 44.00, 56.33, C, "SUN"),
    ("Murmansk", 33.08, 68.97, C, "SUN"),
    ("Arcángel", 40.54, 64.54, C, "SUN"),
    ("Vorónezh", 39.20, 51.66, C, "SUN"),
    ("Dnipropetrovsk", 35.05, 48.46, C, "SUN"),
    ("Stalino", 37.80, 48.00, C, "SUN"),
    ("Kalinin", 35.91, 56.86, C, "SUN"),
    ("Tula", 37.62, 54.19, C, "SUN"),
    ("Oriol", 36.08, 52.97, C, "SUN"),
    ("Briansk", 34.36, 53.24, C, "SUN"),
    ("Vítebsk", 30.20, 55.19, C, "SUN"),
    ("Gómel", 30.98, 52.43, C, "SUN"),
    ("Krasnodar", 38.98, 45.04, C, "SUN"),
    ("Grozni", 45.69, 43.32, C, "SUN"),
    ("Tiflis", 44.79, 41.72, C, "SUN"),
    ("Ereván", 44.51, 40.18, C, "SUN", OPT),
    ("Sarátov", 46.03, 51.53, C, "SUN"),
    ("Yaroslavl", 39.89, 57.63, C, "SUN"),
    ("Vólogda", 39.89, 59.22, C, "SUN"),
    ("Penza", 45.00, 53.20, C, "SUN"),
    ("Tambov", 41.45, 52.72, C, "SUN"),
    ("Nóvgorod", 31.27, 58.52, C, "SUN"),
    ("Pskov", 28.33, 57.82, C, "SUN"),
    ("Viazma", 34.30, 55.21, C, "SUN"),
    ("Kerch", 36.47, 45.36, C, "SUN"),
    ("Nikoláyev", 31.99, 46.97, C, "SUN"),
    ("Poltava", 34.55, 49.59, C, "SUN"),
    ("Vínnytsia", 28.47, 49.23, C, "SUN"),
    ("Zhitómir", 28.66, 50.25, C, "SUN"),
    ("Chernígov", 31.29, 51.49, C, "SUN"),
    ("Maikop", 40.10, 44.60, C, "SUN"),
    ("Petrozavodsk", 34.35, 61.79, C, "SUN"),
    ("Tallin", 24.75, 59.44, C, "SUN-BAL"),
    ("Kaunas", 23.90, 54.90, C, "SUN-BAL"),
    # Poland
    ("Cracovia", 19.94, 50.06, C, "POL"),
    ("Łódź", 19.46, 51.76, C, "POL"),
    ("Poznań", 16.93, 52.41, C, "POL"),
    ("Lwów", 24.03, 49.84, C, "POL"),
    ("Wilno", 25.28, 54.69, C, "POL"),
    ("Lublin", 22.57, 51.25, C, "POL"),
    ("Brest-Litovsk", 23.69, 52.10, C, "POL"),
    ("Gdynia", 18.53, 54.52, C, "POL"),
    ("Białystok", 23.16, 53.13, C, "POL"),
    ("Tarnopol", 25.59, 49.55, C, "POL"),
    ("Pinsk", 26.10, 52.11, C, "POL"),
    ("Równe", 26.25, 50.62, C, "POL"),
    ("Bydgoszcz", 18.00, 53.12, C, "POL"),
    # Yugoslavia
    ("Zagreb", 15.98, 45.81, C, "YUG"),
    ("Liubliana", 14.51, 46.06, C, "YUG"),
    ("Sarajevo", 18.41, 43.86, C, "YUG"),
    ("Split", 16.44, 43.51, C, "YUG"),
    ("Skopie", 21.43, 42.00, C, "YUG"),
    ("Niš", 21.90, 43.32, C, "YUG"),
    ("Banja Luka", 17.19, 44.77, C, "YUG"),
    ("Dubrovnik", 18.09, 42.65, C, "YUG"),
    # Spain
    ("Valencia", -0.38, 39.47, C, "ESP"),
    ("Sevilla", -5.98, 37.39, C, "ESP"),
    ("Bilbao", -2.93, 43.26, C, "ESP"),
    ("Zaragoza", -0.88, 41.65, C, "ESP"),
    ("Málaga", -4.42, 36.72, C, "ESP"),
    ("La Coruña", -8.41, 43.36, C, "ESP"),
    ("Cartagena", -0.99, 37.60, C, "ESP"),
    ("Valladolid", -4.72, 41.65, C, "ESP"),
    ("Badajoz", -6.97, 38.88, C, "ESP"),
    ("Palma", 2.65, 39.57, C, "ESP"),
    ("Melilla", -2.94, 35.29, C, "ESP-MAR"),
    # Turkey
    ("Esmirna", 27.14, 38.42, C, "TUR"),
    ("Adana", 35.32, 37.00, C, "TUR"),
    ("Bursa", 29.06, 40.18, C, "TUR"),
    ("Samsun", 36.33, 41.29, C, "TUR"),
    ("Trebisonda", 39.72, 41.00, C, "TUR"),
    ("Erzurum", 41.27, 39.90, C, "TUR"),
    ("Antalya", 30.71, 36.90, C, "TUR"),
    ("Konya", 32.48, 37.87, C, "TUR"),
    ("Edirne", 26.56, 41.68, C, "TUR"),
    ("Kayseri", 35.49, 38.73, C, "TUR"),
    ("Sivas", 37.02, 39.75, C, "TUR"),
    ("Alejandreta", 36.17, 36.59, C, "TUR"),
    ("Diyarbakır", 40.23, 37.91, C, "TUR"),
    # Sweden
    ("Gotemburgo", 11.97, 57.71, C, "SWE"),
    ("Malmö", 13.00, 55.60, C, "SWE"),
    ("Kiruna", 20.22, 67.86, C, "SWE"),
    ("Luleå", 22.15, 65.58, C, "SWE"),
    ("Sundsvall", 17.31, 62.39, C, "SWE"),
    ("Östersund", 14.64, 63.18, C, "SWE"),
    ("Umeå", 20.26, 63.83, C, "SWE"),
    ("Norrköping", 16.18, 58.59, C, "SWE"),
    ("Karlskrona", 15.59, 56.16, C, "SWE"),
    ("Visby", 18.29, 57.64, C, "SWE"),
    # Switzerland, Ireland, Portugal
    ("Zúrich", 8.54, 47.38, C, "CHE"),
    ("Cork", -8.47, 51.90, C, "IRL"),
    ("Galway", -9.05, 53.27, C, "IRL"),
    ("Oporto", -8.61, 41.15, C, "PRT"),
    ("Faro", -7.93, 37.02, C, "PRT"),
]
# Stable city ids (decision of #4): English exonym slugs where one is common, otherwise the local
# name folded to ASCII. Cities not listed here get the slug of their name.
CITY_IDS = {
    "Roma": "rome", "Bucarest": "bucharest", "Londres": "london", "París": "paris", "Bruselas": "brussels",
    "Atenas": "athens", "Copenhague": "copenhagen", "Moscú": "moscow", "Varsovia": "warsaw",
    "Belgrado": "belgrade", "Estocolmo": "stockholm", "Berna": "bern", "Lisboa": "lisbon", "Rodas": "rhodes",
    "El Cairo": "cairo", "Jerusalén": "jerusalem", "Amán": "amman", "La Valeta": "valletta", "Argel": "algiers",
    "Túnez": "tunis", "Damasco": "damascus", "Tetuán": "tetouan", "Reikiavik": "reykjavik",
    "Stalingrado": "stalingrad", "Leningrado": "leningrad", "Járkov": "kharkov", "Estambul": "istanbul",
    "Róterdam": "rotterdam",
    "Hamburgo": "hamburg", "Múnich": "munich", "Colonia": "cologne", "Fráncfort": "frankfurt",
    "Dresde": "dresden", "Núremberg": "nuremberg", "Viena": "vienna", "Praga": "prague", "Milán": "milan",
    "Turín": "turin", "Génova": "genoa", "Venecia": "venice", "Bolonia": "bologna", "Florencia": "florence",
    "Nápoles": "naples", "Bengasi": "benghazi", "Constanza": "constanta", "Mánchester": "manchester",
    "Edimburgo": "edinburgh", "Alejandría": "alexandria", "Marsella": "marseille", "Tolón": "toulon",
    "Burdeos": "bordeaux", "Estrasburgo": "strasbourg", "Niza": "nice", "Constantina": "constantine",
    "Bona": "bone", "Marrakech": "marrakesh", "Uchda": "oujda", "Alepo": "aleppo", "Amberes": "antwerp",
    "Lieja": "liege", "Luxemburgo": "luxembourg", "Salónica": "thessaloniki", "Ioánina": "ioannina",
    "Heraclión": "heraklion", "Sebastopol": "sevastopol", "Arcángel": "arkhangelsk", "Oriol": "orel",
    "Briansk": "bryansk", "Grozni": "grozny", "Ereván": "yerevan", "Viazma": "vyazma", "Nikoláyev": "nikolaev",
    "Tallin": "tallinn", "Cracovia": "krakow", "Liubliana": "ljubljana", "Skopie": "skopje",
    "Sevilla": "seville", "Esmirna": "smyrna", "Trebisonda": "trebizond", "Alejandreta": "alexandretta",
    "Gotemburgo": "gothenburg", "Oporto": "porto",
}

# A city whose hex is taken or of another territory goes to the nearest free hex of its territory
# within this distance.
CITY_MAX_SHIFT_KM = 135

# Airports without a city: (name, lon, lat). The name is only for documentation.
AIRPORTS = [
    ("Banak", 24.97, 70.07),
    ("Sidi Barrani", 25.92, 31.61),
]
