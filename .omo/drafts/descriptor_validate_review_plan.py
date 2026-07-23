import hashlib
import os
import stat
import sys

root = sys.argv[1]
expected = sys.argv[2]
parts = [".omo", "plans", "complete-rehealth-backend-model-service.md"]
fds: list[int] = []
try:
    fd = os.open(root, os.O_RDONLY | os.O_DIRECTORY | os.O_NOFOLLOW)
    fds.append(fd)
    for part in parts[:-1]:
        fd = os.open(part, os.O_RDONLY | os.O_DIRECTORY | os.O_NOFOLLOW, dir_fd=fd)
        fds.append(fd)
        if not stat.S_ISDIR(os.fstat(fd).st_mode):
            raise RuntimeError(f"not a directory: {part}")
    file_fd = os.open(parts[-1], os.O_RDONLY | os.O_NOFOLLOW, dir_fd=fd)
    fds.append(file_fd)
    if not stat.S_ISREG(os.fstat(file_fd).st_mode):
        raise RuntimeError("target is not a regular file")
    digest = hashlib.sha256()
    while block := os.read(file_fd, 1024 * 1024):
        digest.update(block)
    actual = digest.hexdigest()
    if actual != expected:
        raise RuntimeError(f"digest mismatch: {actual}")
    print(actual)
finally:
    for open_fd in reversed(fds):
        os.close(open_fd)
