import json
import os
import sys

LEARN_DIR = "app/src/main/assets/learn"
DATA_DIR = "scripts/data"

IMPLICITLY_SAHIH = {"bukhari", "muslim"}

def extract_grade(hadith, collection):
    if collection in IMPLICITLY_SAHIH:
        return "Sahih"
    grades = hadith.get("grades", [])
    # Prefer Al-Albani grading, then any grader
    for grader_pref in ["Al-Albani", "Shu'ayb Al Arna'ut"]:
        for g in grades:
            if grader_pref in g.get("name", ""):
                return g.get("grade", "")
    if grades:
        return grades[0].get("grade", "")
    return None

def main():
    # 1. Load quran-uthmani.json -> build index: (surah_int, ayah_int) → arabic_text
    print("📦 Loading Quran Arabic (Uthmani)...")
    with open(os.path.join(DATA_DIR, "quran-uthmani.json"), 'r', encoding='utf-8') as f:
        quran_uthmani_raw = json.load(f)
    
    quran_arabic_index = {}
    for surah_str, ayahs_list in quran_uthmani_raw.items():
        surah_int = int(surah_str)
        for ayah_data in ayahs_list:
            quran_arabic_index[(surah_int, ayah_data['verse'])] = ayah_data['text']

    # 2. Load en.quran.json -> build index: (surah_int, ayah_int) → english_text
    print("📦 Loading Quran English...")
    with open(os.path.join(DATA_DIR, "en.quran.json"), 'r', encoding='utf-8') as f:
        en_quran_raw = json.load(f)
    
    quran_english_index = {}
    for entry in en_quran_raw['quran']:
        quran_english_index[(entry['chapter'], entry['verse'])] = entry['text']

    # 3. Load hadith collections -> build index: collection_name → {number → hadith_obj}
    hadith_collections = {}
    hadith_files = [f for f in os.listdir(DATA_DIR) if f.startswith("en.") and f.endswith(".json") and f != "en.quran.json"]
    for h_file in hadith_files:
        coll_name = h_file.split(".")[1]
        print(f"📦 Loading Hadith collection: {coll_name}...")
        with open(os.path.join(DATA_DIR, h_file), 'r', encoding='utf-8') as f:
            h_data = json.load(f)
            hadith_collections[coll_name] = {h['hadithnumber']: h for h in h_data['hadiths']}

    # 4. Load tajweed_overrides.json -> build index: "surah:ayah" → tajweed_markup_string
    print("📦 Loading Tajweed overrides...")
    tajweed_overrides = {}
    overrides_path = os.path.join(DATA_DIR, "tajweed_overrides.json")
    if os.path.exists(overrides_path):
        with open(overrides_path, 'r', encoding='utf-8') as f:
            tajweed_overrides = json.load(f)

    # 5. Process each JSON file in assets/learn/
    files = sorted(f for f in os.listdir(LEARN_DIR) if f.endswith(".json"))
    for filename in files:
        file_path = os.path.join(LEARN_DIR, filename)
        with open(file_path, 'r', encoding='utf-8') as f:
            topic = json.load(f)
        
        modified = False
        print(f"📄 Processing {filename}...")
        
        for block in topic.get("blocks", []):
            if block['type'] == 'ayah':
                surah = block['surah']
                ayah = block['ayah']
                ref = (surah, ayah)
                
                text_uthmani = quran_arabic_index.get(ref)
                translation_en = quran_english_index.get(ref)
                
                if block.get("verified"):
                    if not text_uthmani:
                        print(f"❌ ERROR: Ayah {surah}:{ayah} Arabic text not found!")
                        sys.exit(1)
                    if not translation_en:
                        print(f"❌ ERROR: Ayah {surah}:{ayah} English translation not found!")
                        sys.exit(1)
                
                block['textUthmani'] = text_uthmani
                block['translationEn'] = translation_en
                block['tajweedMarkup'] = tajweed_overrides.get(f"{surah}:{ayah}")
                modified = True
                
            elif block['type'] == 'hadith':
                coll = block['collection']
                num = block['number']
                
                hadith_obj = hadith_collections.get(coll, {}).get(num)
                
                if block.get("verified"):
                    if not hadith_obj:
                        print(f"❌ ERROR: Hadith {coll} {num} not found!")
                        sys.exit(1)
                
                if hadith_obj:
                    block['textEn'] = hadith_obj.get('text')
                    block['grade'] = extract_grade(hadith_obj, coll)
                    block['narrator'] = hadith_obj.get('narrator')
                    block['chapter'] = hadith_obj.get('chaptername')
                    modified = True

        if modified:
            with open(file_path, 'w', encoding='utf-8') as f:
                json.dump(topic, f, ensure_ascii=False, indent=2)

    print("✅ Seeding complete.")

if __name__ == "__main__":
    main()
