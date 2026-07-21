#!/bin/bash
set -e

echo "=== Compute JeecgBoot PBEWithMD5AndDES via model-service Python ==="
# The model-service container is Python-based and should have cryptography

docker exec rehealth-staging-model-service-1 python3 << 'PYEOF'
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
from cryptography.hazmat.primitives import padding as sym_padding
import hashlib

def md5(data):
    return hashlib.md5(data).digest()

def pbe_md5_and_des(plaintext, password, salt_str, iterations=1000):
    """
    Java PBEWithMD5AndDES as used by JeecgBoot PasswordUtil.encrypt:
    - password (PBE key) = username string
    - salt = user's salt string from DB
    - plaintext = the password to encrypt
    
    Key derivation (Java PBKDF1-like for PBEWithMD5AndDES):
    dk = MD5(password_bytes + salt_bytes)  # 16 bytes for DES
    key = dk[0:8], iv = dk[8:16]
    
    Then DES-CBC encrypt with PKCS5 padding, 1000 iterations on cipher init.
    NOTE: The "iterations" in Java PBEWithMD5AndDES refers to the iteration count
    passed to PBEParameterSpec, which controls how many times the cipher 
    encrypts in a loop.
    """
    pwd_bytes = password.encode('utf-8')
    salt_bytes = salt_str.encode('utf-8')
    plain_bytes = plaintext.encode('utf-8')
    
    # PBKDF1 key derivation
    dk = md5(pwd_bytes + salt_bytes)
    key = dk[:8]
    iv = dk[8:16]
    
    # PKCS7 padding
    padder = sym_padding.PKCS7(8).padder()
    padded = padder.update(plain_bytes) + padder.finalize()
    
    # DES-CBC encrypt
    cipher = Cipher(algorithms.DES(key), modes.CBC(iv))
    encryptor = cipher.encryptor()
    encrypted = encryptor.update(padded) + encryptor.finalize()
    
    return encrypted.hex()

username = "13507007984"
password = "123456"
salt = "APDzGLuO"

hash_val = pbe_md5_and_des(password, username, salt, 1000)
print(f"PasswordUtil.encrypt('{username}', '{password}', '{salt}')")
print(f"HASH={hash_val}")
print(f"Length={len(hash_val)}")
PYEOF
