import os
import glob
import re

css_files = glob.glob('src/main/resources/styles/*.css')

def replace_all(content):
    # Fix topics-hero background to match sidebar (vibrant green)
    content = re.sub(r'\.topics-hero\s*\{[^\}]*-fx-background-color:\s*#[a-zA-Z0-9]+;[^\}]*\}', 
                     '.topics-hero {\n    -fx-background-color: linear-gradient(to right, #2E7D32, #1B5E20);\n    -fx-background-radius: 16;\n    -fx-padding: 24 32;\n    -fx-effect: dropshadow(gaussian, rgba(46, 125, 50, 0.3), 20, 0.2, 0, 8);\n    -fx-max-width: Infinity;\n}', content)
    
    # Same for social-hero
    content = re.sub(r'\.social-hero\s*\{[^\}]*-fx-background-color:\s*#[a-zA-Z0-9]+;[^\}]*\}', 
                     '.social-hero {\n    -fx-background-color: linear-gradient(to right, #2E7D32, #1B5E20);\n    -fx-background-radius: 12;\n    -fx-padding: 24 32;\n    -fx-effect: dropshadow(gaussian, rgba(46, 125, 50, 0.3), 15, 0, 0, 5);\n}', content)

    # Fix New Topic Button (btn-nav-accent)
    content = re.sub(r'\.btn-nav-accent\s*\{[^\}]*\}', 
                     '.btn-nav-accent {\n    -fx-background-color: #FFFFFF;\n    -fx-text-fill: #2E7D32;\n    -fx-font-weight: bold;\n    -fx-padding: 10 24;\n    -fx-background-radius: 8;\n    -fx-cursor: hand;\n    -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.1), 10, 0.2, 0, 4);\n}', content)
    
    content = re.sub(r'\.btn-nav-accent:hover\s*\{[^\}]*\}', 
                     '.btn-nav-accent:hover {\n    -fx-background-color: #F1F8E9;\n    -fx-text-fill: #1B5E20;\n}', content)

    # Fix feed tabs active state
    content = re.sub(r'\.feed-tab:selected,\s*\.feed-tab:pressed\s*\{[^\}]*\}', 
                     '.feed-tab:selected, .feed-tab:pressed {\n    -fx-background-color: #2E7D32;\n    -fx-text-fill: white;\n}', content)
                     
    # Fix category pill
    content = re.sub(r'\.category-pill\s*\{[^\}]*\}', 
                     '.category-pill {\n    -fx-background-color: #E8F5E9;\n    -fx-text-fill: #2E7D32;\n    -fx-padding: 4 10;\n    -fx-background-radius: 12;\n    -fx-font-size: 11px;\n    -fx-font-weight: 700;\n}', content)
                     
    # Fix badge count
    content = re.sub(r'\.search-count-badge\s*\{[^\}]*\}', 
                     '.search-count-badge {\n    -fx-background-color: #E8F5E9;\n    -fx-text-fill: #2E7D32;\n    -fx-font-size: 13px;\n    -fx-font-weight: bold;\n    -fx-padding: 8 16;\n    -fx-background-radius: 8;\n}', content)

    return content


for file_path in css_files:
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original = content
    content = replace_all(content)
        
    if original != content:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"Updated {file_path}")

print("Done.")
