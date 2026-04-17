import json
import os
import sys
import sqlite3

LEARN_DIR = "app/src/main/assets/learn"
DATA_DIR = "scripts/data"
OUTPUT_DB = "app/src/main/assets/khushu.db"

# These collections are Sahih by definition in the Fawaz Ahmed dataset
IMPLICITLY_SAHIH = {"bukhari", "muslim"}

def build_db():
    if os.path.exists(OUTPUT_DB):
        os.remove(OUTPUT_DB)
    
    conn = sqlite3.connect(OUTPUT_DB)
    cursor = conn.cursor()

    # Create tables
    cursor.execute("""
        CREATE TABLE surahs (
            number INTEGER PRIMARY KEY,
            name_arabic TEXT,
            name_en TEXT,
            name_translation TEXT,
            ayah_count INTEGER,
            revelation_type TEXT
        )
    """)

    cursor.execute("""
        CREATE TABLE ayahs (
            surah INTEGER,
            ayah INTEGER,
            text_uthmani TEXT,
            tajweed_markup TEXT,
            PRIMARY KEY (surah, ayah),
            FOREIGN KEY (surah) REFERENCES surahs(number)
        )
    """)
    cursor.execute("CREATE INDEX idx_ayahs_ref ON ayahs(surah, ayah)")

    cursor.execute("""
        CREATE TABLE hadiths (
            collection TEXT,
            number INTEGER,
            text_arabic TEXT,
            text_en TEXT,
            grade TEXT,
            narrator TEXT,
            PRIMARY KEY (collection, number)
        )
    """)
    cursor.execute("CREATE INDEX idx_hadiths_ref ON hadiths(collection, number)")

    cursor.execute("""
        CREATE TABLE translations (
            surah INTEGER,
            ayah INTEGER,
            lang TEXT,
            text TEXT,
            edition TEXT,
            PRIMARY KEY (surah, ayah, lang, edition)
        )
    """)

    # Collect unique references from topic JSONs
    ayah_refs = set()
    hadith_refs = set()

    files = sorted(f for f in os.listdir(LEARN_DIR) if f.endswith(".json"))
    for filename in files:
        with open(os.path.join(LEARN_DIR, filename), 'r', encoding='utf-8') as f:
            topic = json.load(f)
        
        for block in topic.get("blocks", []):
            if block['type'] == 'ayah':
                ayah_refs.add((block['surah'], block['ayah']))
            elif block['type'] == 'hadith':
                if not block.get("verified"):
                    print(f"❌ ERROR: Hadith {block['display']} in {filename} is NOT verified!")
                    sys.exit(1)
                hadith_refs.add((block['collection'], block['number']))

    # Load Tajweed Overrides
    tajweed_overrides = {}
    overrides_path = os.path.join(DATA_DIR, "tajweed_overrides.json")
    if os.path.exists(overrides_path):
        with open(overrides_path, 'r', encoding='utf-8') as f:
            tajweed_overrides = json.load(f)

    # Load Tanzil data
    print("📦 Processing Quran data...")
    with open(os.path.join(DATA_DIR, "quran-uthmani.json"), 'r', encoding='utf-8') as f:
        tanzil_data = json.load(f)

    # Process Surahs and Ayahs
    surahs_to_insert = set()
    for surah_str, ayahs_list in tanzil_data.items():
        surah_num = int(surah_str)
        surahs_to_insert.add(surah_num)
        
        for ayah_data in ayahs_list:
            ayah_num = ayah_data['verse']
            if (surah_num, ayah_num) in ayah_refs:
                key = f"{surah_num}:{ayah_num}"
                markup = tajweed_overrides.get(key)
                cursor.execute(
                    "INSERT INTO ayahs (surah, ayah, text_uthmani, tajweed_markup) VALUES (?, ?, ?, ?)",
                    (surah_num, ayah_num, ayah_data['text'], markup)
                )
    
    # Metadata for Surahs
    for surah_num in sorted(list(surahs_to_insert)):
        cursor.execute(
            "INSERT INTO surahs (number, name_arabic, name_en, name_translation, ayah_count, revelation_type) VALUES (?, ?, ?, ?, ?, ?)",
            (surah_num, "", "", "", 0, "")
        )

    # Load Hadith data
    print("📦 Processing Hadith data...")
    collections = {}
    for coll_name, _ in hadith_refs:
        if coll_name not in collections:
            file_path = os.path.join(DATA_DIR, f"en.{coll_name}.json")
            if os.path.exists(file_path):
                with open(file_path, 'r', encoding='utf-8') as f:
                    data = json.load(f)
                    collections[coll_name] = {h['hadithnumber']: h for h in data['hadiths']}
    
    for coll_name, hadith_num in hadith_refs:
        h = collections.get(coll_name, {}).get(hadith_num)
        if h:
            if coll_name in IMPLICITLY_SAHIH:
                grade_str = "Sahih"
            else:
                sahih_grades = [g['grade'] for g in h.get('grades', []) if 'sahih' in g['grade'].lower()]
                if not sahih_grades:
                    print(f"❌ ERROR: Hadith {coll_name} {hadith_num} is not Sahih!")
                    sys.exit(1)
                grade_str = sahih_grades[0]
            
            cursor.execute(
                "INSERT INTO hadiths (collection, number, text_en, grade, narrator) VALUES (?, ?, ?, ?, ?)",
                (coll_name, hadith_num, h['text'], grade_str, "")
            )

    conn.commit()
    conn.close()
    print(f"✅ Database built successfully: {OUTPUT_DB}")

if __name__ == "__main__":
    build_db()
