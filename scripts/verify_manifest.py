"""
verify_manifest.py — validates hadith/ayah references in assets/learn/*.json
against local Fawaz Ahmed and Quran JSON datasets.

Run from project root:
    python3 scripts/verify_manifest.py

Formats confirmed from actual downloaded files:
  Quran:   dict keyed by surah str -> list of {chapter, verse, text}
  Hadith:  {metadata: {...}, hadiths: [{hadithnumber, text, grades: [{name, grade}], ...}]}
  Grades:  bukhari/muslim are implicitly Sahih (grades array empty by design).
           Other collections: accept if ANY grader says Sahih.
"""

import json, os, sys

LEARN_DIR = "app/src/main/assets/learn"
DATA_DIR  = "scripts/data"

# These collections are Sahih by definition — individual grade check not needed
IMPLICITLY_SAHIH = {"bukhari", "muslim"}


def load_quran():
    path = os.path.join(DATA_DIR, "quran-uthmani.json")
    if not os.path.exists(path):
        print(f"quran-uthmani.json not found in {DATA_DIR}/"); sys.exit(1)
    raw = json.load(open(path, encoding="utf-8"))
    # Format: {"1": [{chapter, verse, text}, ...], "2": [...], ...}
    index = {}
    for surah_str, ayahs in raw.items():
        for a in ayahs:
            index[(int(surah_str), a["verse"])] = a["text"]
    return index


def load_hadith_collection(collection):
    path = os.path.join(DATA_DIR, f"en.{collection}.json")
    if not os.path.exists(path):
        return None
    raw = json.load(open(path, encoding="utf-8"))
    return {h["hadithnumber"]: h for h in raw.get("hadiths", [])}


def is_sahih(hadith, collection):
    if collection in IMPLICITLY_SAHIH:
        return True
    grades = hadith.get("grades", [])
    return any("sahih" in g.get("grade", "").lower() for g in grades)


def verify_topics(quran_index, hadith_cache):
    files = sorted(f for f in os.listdir(LEARN_DIR) if f.endswith(".json"))
    total = ok = warn = err = 0

    for filename in files:
        path  = os.path.join(LEARN_DIR, filename)
        topic = json.load(open(path, encoding="utf-8"))
        changed = False
        print(f"\n-- {topic.get('title', filename)} --")

        for block in topic.get("blocks", []):
            btype = block.get("type")

            if btype == "ayah":
                total += 1
                surah, ayah = block["surah"], block["ayah"]
                text = quran_index.get((surah, ayah))
                if text:
                    print(f"  OK  Ayah {surah}:{ayah}  {text[:80]}...")
                    block["verified"] = True
                    changed = True; ok += 1
                else:
                    print(f"  XX  Ayah {surah}:{ayah} -- NOT FOUND in Quran dataset")
                    err += 1

            elif btype == "hadith":
                total += 1
                collection = block["collection"]
                number     = block["number"]
                display    = block.get("display", f"{collection} {number}")

                if collection not in hadith_cache:
                    hadith_cache[collection] = load_hadith_collection(collection)

                db = hadith_cache[collection]
                if db is None:
                    print(f"  XX  {display} -- en.{collection}.json not downloaded"); err += 1; continue

                hadith = db.get(number)
                if not hadith:
                    print(f"  XX  {display} -- number {number} NOT FOUND"); err += 1
                elif not is_sahih(hadith, collection):
                    grades = [g.get("grade") for g in hadith.get("grades", [])]
                    print(f"  !!  {display} -- grade is {grades}, not Sahih"); warn += 1
                else:
                    text = hadith.get("text", "")
                    print(f"  OK  {display}")
                    print(f"      {text[:150]}...")
                    block["verified"] = True
                    changed = True; ok += 1

        if changed:
            json.dump(topic, open(path, "w", encoding="utf-8"), indent=2, ensure_ascii=False)

    print(f"\n{'--'*30}")
    print(f"Total: {total}  OK: {ok}  WARN: {warn}  ERR: {err}")
    if warn or err:
        print("Review !! and XX entries -- these remain unverified.")
    else:
        print("All references verified. Ready to run seeder.py")


if __name__ == "__main__":
    verify_topics(load_quran(), {})
