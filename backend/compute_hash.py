#!/usr/bin/env python3
"""
Compute JeecgBoot PBEWithMD5AndDES password hash.
LoginController line 686: PasswordUtil.encrypt(username, password, sysUser.getSalt())
"""
from Crypto.Cipher import DES
from Crypto.Hash import MD5
import struct, binascii

def pbe_with_md5_and_des(plaintext: str, password: str, salt_str: str, iterations=1000):
    """
    JeecgBoot PasswordUtil.encrypt uses PBEWithMD5AndDES:
    - Key derived from password via PBEKeySpec (password chars as key)
    - Salt from salt_str.getBytes()
    - Iteration count = 1000
    - Then DES encrypt the plaintext
    """
    salt = salt_str.encode('utf-8')
    pwd_bytes = password.encode('utf-8')
    plain_bytes = plaintext.encode('utf-8')

    # PBE key derivation: MD5-based
    # Java PBEWithMD5AndDES uses PKCS5 key derivation from password
    # MD5(password + salt), repeat until we have enough key material
    
    # For PBEWithMD5AndDES, the key derivation is:
    # 1. Mix in the password
    # 2. Mix in the salt
    # 3. Produce 8 bytes key + 8 bytes IV
    # This follows the PKCS#5 v1.5 scheme (but Java's implementation is specific)
    
    # Actually, Java's PBEWithMD5AndDES SecretKeyFactory.generateSecret(PBEKeySpec)
    # uses: PBKDF1-like algorithm
    # dk0 = MD5(password_bytes + salt)
    # dk1 = MD5(dk0 + salt)   (if needed for key + IV)
    
    dk0 = MD5.new(pwd_bytes + salt).digest()
    dk1 = MD5.new(dk0 + salt).digest()
    
    key_material = dk0 + dk1
    key = key_material[:8]  # DES key (8 bytes)
    iv = key_material[8:16]  # IV (8 bytes)
    
    # PKCS5 padding
    pad_len = 8 - (len(plain_bytes) % 8)
    padded = plain_bytes + bytes([pad_len] * pad_len)
    
    cipher = DES.new(key, DES.MODE_CBC, iv)
    encrypted = cipher.encrypt(padded)
    
    return binascii.hexlify(encrypted).decode('ascii').lower()


# The user: username="13507007984", password="123456", salt="APDzGLuO"
username = "13507007984"
password = "123456"
salt = "APDzGLuO"

result = pbe_with_md5_and_des(password, username, salt)
print(f"PasswordUtil.encrypt('{username}', '{password}', '{salt}')")
print(f"= {result}")
print(f"Length: {len(result)}")
