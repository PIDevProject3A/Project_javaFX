import os
import glob

replacements = {
    '#1E1B4B': '#1B5E20',
    '#A5B4FC': '#A5D6A7',
    '#4F46E5': '#2E7D32',
    '#4338CA': '#256b2a',
    '#EEF2FF': '#F1F8E9',
    '#C7D2FE': '#cce2c7',
    '#E0E7FF': '#d8f0d9',
    '#312E81': '#134015'
}

css_files = glob.glob('src/main/resources/styles/*.css')

for file_path in css_files:
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original = content
    for old, new in replacements.items():
        content = content.replace(old, new)
        content = content.replace(old.lower(), new)
        
    if original != content:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"Updated {file_path}")

print("Done.")
