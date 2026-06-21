import os
import glob
import re

src_dir = 'app/src/main/java/com/moment/app/ui'
files = glob.glob(f'{src_dir}/**/*.kt', recursive=True)

# Replacements mapping
replacements = {
    # Replace HeartRed as container or tint with primary
    r'containerColor\s*=\s*HeartRed': 'containerColor = MaterialTheme.colorScheme.primary',
    r'tint\s*=\s*HeartRed': 'tint = MaterialTheme.colorScheme.primary',
    r'color\s*=\s*HeartRed': 'color = MaterialTheme.colorScheme.primary',
    r'ambientColor\s*=\s*HeartRed': 'ambientColor = MaterialTheme.colorScheme.primary',
    r'background\(HeartRed\)': 'background(MaterialTheme.colorScheme.primary)',
    
    # Replace TextDeep
    r'color\s*=\s*TextDeep': 'color = MaterialTheme.colorScheme.onSurface',
    r'tint\s*=\s*TextDeep': 'tint = MaterialTheme.colorScheme.onSurface',
    
    # Replace TextMuted
    r'color\s*=\s*TextMuted': 'color = MaterialTheme.colorScheme.onSurfaceVariant',
    r'tint\s*=\s*TextMuted': 'tint = MaterialTheme.colorScheme.onSurfaceVariant',
    
    # Replace White for backgrounds/containers
    r'containerColor\s*=\s*White': 'containerColor = MaterialTheme.colorScheme.surface',
    r'background\(White\)': 'background(MaterialTheme.colorScheme.surface)',
    
    # Replace WarmBeige for borders or unused backgrounds
    r'unfocusedBorderColor\s*=\s*WarmBeige': 'unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant',
    r'dragHandle\s*=\s*BottomSheetDefaults\.DragHandle\(color\s*=\s*WarmBeige\)': 'dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.surfaceVariant) }',
    
    # Replace ErrorSoft
    r'containerColor\s*=\s*ErrorSoft': 'containerColor = MaterialTheme.colorScheme.error',
    r'color\s*=\s*ErrorSoft': 'color = MaterialTheme.colorScheme.error',
    r'tint\s*=\s*ErrorSoft': 'tint = MaterialTheme.colorScheme.error',
}

for filepath in files:
    with open(filepath, 'r') as f:
        content = f.read()
        
    original_content = content
    
    for pattern, replacement in replacements.items():
        content = re.sub(pattern, replacement, content)
        
    if content != original_content:
        # Check if MaterialTheme is imported
        if 'androidx.compose.material3.MaterialTheme' not in content:
            # Insert import after the package declaration
            content = re.sub(r'package\s+.*?\n', r'\g<0>\nimport androidx.compose.material3.MaterialTheme\n', content, count=1)
            
        with open(filepath, 'w') as f:
            f.write(content)
        print(f"Updated {filepath}")
