import sys
fn = sys.argv[1]
with open(fn, encoding='utf-8') as f:
    lines = f.readlines()
depth = {'(': 0, '[': 0, '{': 0}
in_block = False
in_str1 = False
in_str2 = False
in_str3 = False
stack = []  # (char, line)
for i, raw in enumerate(lines, 1):
    line = raw
    j = 0
    while j < len(line):
        c = line[j]
        nxt = line[j+1] if j+1 < len(line) else ''
        if in_str3:
            if c == '"' and nxt == '"':
                in_str3 = False; j += 2; continue
            elif c == '"':
                in_str3 = False; j += 1; continue
            j += 1; continue
        if in_str2:
            if c == '"': in_str2 = False
            elif c == '\\': j += 2; continue
            j += 1; continue
        if in_str1:
            if c == "'": in_str1 = False
            elif c == '\\': j += 2; continue
            j += 1; continue
        if in_block:
            if c == '*' and nxt == '/': in_block = False; j += 2; continue
            j += 1; continue
        if c == '/' and nxt == '/': break
        if c == '/' and nxt == '*': in_block = True; j += 2; continue
        if c == '"' and nxt == '"':
            if j+2 < len(line) and line[j+2] == '"':
                in_str3 = True; j += 3; continue
            in_str2 = True; j += 1; continue
        if c == "'": in_str1 = True; j += 1; continue
        if c in '([{':
            depth[c] += 1; stack.append((c, i))
        elif c in ')]}':
            o = '(' if c == ')' else ('[' if c == ']' else '{')
            if depth[o] == 0:
                print(f"LINE {i}: close '{c}' with NO open '{o}' (mismatch)")
            else:
                depth[o] -= 1
                if stack: stack.pop()
        j += 1
print("END net open:", {k: depth[k] for k in depth})
