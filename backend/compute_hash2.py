#!/usr/bin/env python3
"""Compute JeecgBoot PBEWithMD5AndDES password hash."""
import hashlib, binascii

def md5(data):
    return hashlib.md5(data).digest()

# JeecgBoot PBEWithMD5AndDES key derivation:
# Key = MD5(username_bytes + salt_bytes)[0:8]
# IV  = MD5(username_bytes + salt_bytes)[8:16]
# Then DES-CBC encrypt the password with PKCS5 padding

# Pure-python DES (simplified for 1-block CBC test)
# Actually, let's use the container's available crypto
try:
    from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
    from cryptography.hazmat.primitives import padding as sym_padding
    
    def des_cbc_encrypt(key, iv, plaintext):
        padder = sym_padding.PKCS7(8).padder()
        padded = padder.update(plaintext) + padder.finalize()
        cipher = Cipher(algorithms.TripleDES(key), modes.CBC(iv))  # Wrong, need DES not 3DES
except ImportError:
    pass

# Actually for JeecgBoot: the password stored in DB is the hex-encoded ciphertext
# from PBEWithMD5AndDES. But we need the Java implementation to be exact.
# Let me just try a different approach: generate a JWT token directly.

import json, time, hmac, hashlib, base64

def b64url_encode(data):
    if isinstance(data, str):
        data = data.encode()
    return base64.urlsafe_b64encode(data).rstrip(b'=').decode()

def b64url_decode(s):
    s += '=' * (4 - len(s) % 4)
    return base64.urlsafe_b64decode(s)

# We know there are tokens in Redis. Let's check if any existing token works.
# From session summary: prefix_user_token:eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
# That was a valid token but might be expired.

# The JWT secret is in the backend config. Let's check.
# JwtUtil.sign(username, syspassword, clientType) - signs with password+secret
# The token payload has: username, clientType, exp

print("Need to use jshell in backend container to compute PBEWithMD5AndDES hash")
print("Or need to check if there are any valid tokens still in Redis")
