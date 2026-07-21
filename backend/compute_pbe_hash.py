import hashlib
import struct
import os

def md5(data):
    return hashlib.md5(data).digest()

def pbe_md5_des_encrypt(plaintext, password, salt_str, iterations=1000):
    """
    Java PBEWithMD5AndDES key derivation (PKCS5 Scheme 1):
    dk = password_bytes + salt_bytes
    key_material = MD5(dk)  -- gives 16 bytes for DES (8 key + 8 IV)
    
    Then DES-CBC encrypt with PKCS5 padding.
    """
    pwd_bytes = password.encode('utf-8')
    salt = salt_str.encode('utf-8')
    plain_bytes = plaintext.encode('utf-8')
    
    # PKCS5 key derivation
    key_material = md5(pwd_bytes + salt)
    
    key = key_material[:8]
    iv = key_material[8:16]
    
    # DES-CBC with PKCS5 padding
    pad_len = 8 - (len(plain_bytes) % 8)
    padded = plain_bytes + bytes([pad_len] * pad_len)
    
    # DES implementation
    from des import des_encrypt_cbc
    encrypted = des_encrypt_cbc(key, iv, padded)
    
    return encrypted.hex()

# Pure Python DES implementation
S_BOXES = [
    # S1
    [14,4,13,1,2,15,11,8,3,10,6,12,5,9,0,7, 0,15,7,4,14,2,13,1,10,6,12,11,9,5,3,8,
     4,1,14,8,13,6,2,11,15,12,9,7,3,10,5,0, 15,12,8,2,4,9,1,7,5,11,3,14,10,0,6,13],
    # S2
    [15,1,8,14,6,11,3,4,9,7,2,13,12,0,5,10, 3,13,4,7,15,2,8,14,12,0,1,10,6,9,11,5,
     0,14,7,11,10,4,13,1,5,8,12,6,9,3,2,15, 13,8,10,1,3,15,4,2,11,6,7,12,0,5,14,9],
    # S3
    [10,0,9,14,6,3,15,5,1,13,12,7,11,4,2,8, 13,7,0,9,3,4,6,10,2,8,5,14,12,11,15,1,
     13,6,4,9,8,15,3,0,11,1,2,12,5,10,14,7, 1,10,13,0,6,9,8,7,4,15,14,3,11,5,2,12],
    # S4
    [7,13,14,3,0,6,9,10,1,2,8,5,11,12,4,15, 13,8,11,5,6,15,0,3,4,7,2,12,1,10,14,9,
     10,6,9,0,12,11,7,13,15,1,3,14,5,2,8,4, 3,15,0,6,10,1,13,8,9,4,5,11,12,7,2,14],
    # S5
    [2,12,4,1,7,10,11,6,8,5,3,15,13,0,14,9, 14,11,2,12,4,7,13,1,5,0,15,10,3,9,8,6,
     4,2,1,11,10,13,7,8,15,9,12,5,6,3,0,14, 11,8,12,7,1,14,2,13,6,15,0,9,10,4,5,3],
    # S6
    [12,1,10,15,9,2,6,8,0,13,3,4,14,7,5,11, 10,15,4,2,7,12,9,5,6,1,13,14,0,11,3,8,
     9,14,15,5,2,8,12,3,7,0,4,10,1,13,11,6, 4,3,2,12,9,5,15,10,11,14,1,7,6,0,8,13],
    # S7
    [4,11,2,14,15,0,8,13,3,12,9,7,5,10,6,1, 13,0,11,7,4,9,1,10,14,3,5,12,2,15,8,6,
     1,4,11,13,12,3,7,14,10,15,6,8,0,5,9,2, 6,11,13,8,1,4,10,7,9,5,0,15,14,2,3,12],
    # S8
    [13,2,8,4,6,15,11,1,10,9,3,14,5,0,12,7, 1,15,13,8,10,3,7,4,12,5,6,2,0,14,9,11,
     7,11,4,1,9,12,14,2,0,6,10,13,15,3,5,8, 2,1,14,7,4,10,8,13,15,12,9,0,3,5,6,11],
]

IP = [58,50,42,34,26,18,10,2,60,52,44,36,28,20,12,4,
      62,54,46,38,30,22,14,6,64,56,48,40,32,24,16,8,
      57,49,41,33,25,17,9,1,59,51,43,35,27,19,11,3,
      61,53,45,37,29,21,13,5,63,55,47,39,31,23,15,7]

FP = [40,8,48,16,56,24,64,32,39,7,47,15,55,23,63,31,
      38,6,46,14,54,22,62,30,37,5,45,13,53,21,61,29,
      36,4,44,12,52,20,60,28,35,3,43,11,51,19,59,27,
      34,2,42,10,50,18,58,26,33,1,41,9,49,17,57,25]

E = [32,1,2,3,4,5,4,5,6,7,8,9,8,9,10,11,12,13,
     12,13,14,15,16,17,16,17,18,19,20,21,20,21,22,23,24,25,
     24,25,26,27,28,29,28,29,30,31,32,1]

P = [16,7,20,21,29,12,28,17,1,15,23,26,5,18,31,10,
     2,8,24,14,32,27,3,9,19,13,30,6,22,11,4,25]

PC1 = [57,49,41,33,25,17,9,1,58,50,42,34,26,18,10,2,
       59,51,43,35,27,19,11,3,60,52,44,36,63,55,47,39,
       31,23,15,7,62,54,46,38,30,22,14,6,61,53,45,37,
       29,21,13,5,28,20,12,4]

PC2 = [14,17,11,24,1,5,3,28,15,6,21,10,23,19,12,4,
       26,8,16,7,27,20,13,2,41,52,31,37,47,55,30,40,
       51,45,33,48,44,49,39,56,34,53,46,42,50,36,29,32]

SHIFTS = [1,1,2,2,2,2,2,2,1,2,2,2,2,2,2,1]

def permute(block, table, n):
    result = 0
    for i in range(len(table)):
        bit = (block >> (n - table[i])) & 1
        result = (result << 1) | bit
    return result

def left_rotate_28(val, n):
    return ((val << n) | (val >> (28 - n))) & 0x0FFFFFFF

def des_keys(key_64):
    key_56 = permute(key_64, PC1, 64)
    C = (key_56 >> 28) & 0x0FFFFFFF
    D = key_56 & 0x0FFFFFFF
    subkeys = []
    for i in range(16):
        C = left_rotate_28(C, SHIFTS[i])
        D = left_rotate_28(D, SHIFTS[i])
        CD = (C << 28) | D
        subkeys.append(permute(CD, PC2, 56))
    return subkeys

def des_f(right, subkey):
    expanded = permute(right, E, 32)
    xored = expanded ^ subkey
    result = 0
    for i in range(8):
        chunk = (xored >> (42 - 6*i)) & 0x3F
        row = ((chunk >> 5) & 1) | ((chunk & 1) << 1)
        col = (chunk >> 1) & 0xF
        val = S_BOXES[i][row * 16 + col]
        result = (result << 4) | val
    return permute(result, P, 32)

def des_block_encrypt(block_64, subkeys):
    block = permute(block_64, IP, 64)
    L = (block >> 32) & 0xFFFFFFFF
    R = block & 0xFFFFFFFF
    for i in range(16):
        new_R = L ^ des_f(R, subkeys[i])
        L = R
        R = new_R
    combined = (R << 32) | L
    return permute(combined, FP, 64)

def des_encrypt_cbc(key_8, iv_8, data):
    key_val = int.from_bytes(key_8, 'big')
    iv_val = int.from_bytes(iv_8, 'big')
    subkeys = des_keys(key_val)
    
    result = b''
    prev = iv_val
    for i in range(0, len(data), 8):
        block = int.from_bytes(data[i:i+8], 'big')
        block ^= prev
        encrypted = des_block_encrypt(block, subkeys)
        result += encrypted.to_bytes(8, 'big')
        prev = encrypted
    return result

# JeecgBoot PasswordUtil.encrypt(username, password, salt)
# From LoginController.java line 686:
#   String userpassword = PasswordUtil.encrypt(username, password, sysUser.getSalt());
# From PasswordUtil.encrypt(plaintext, password, salt):
#   plaintext = login password, password = username (used as PBE key), salt = user salt
#   ALGORITHM = "PBEWithMD5AndDES", ITERATIONCOUNT = 1000
#   Key derived from getPbeKey(password) which uses SecretKeyFactory with PBEKeySpec
#   Then PBEParameterSpec(salt.getBytes(), 1000)
#
# NOTE: Java's PBEWithMD5AndDES key derivation is NOT just MD5(pwd+salt).
# Java uses PBKDF1-like: dk = MD5(password || salt) repeated for key+iv
# With iterations=1000 on the cipher init (NOT on key derivation).
# The key is derived in one step, then cipher uses 1000 iterations of DES-CBC.

def pbe_md5_des(password, plaintext, salt_str, iterations=1000):
    """
    JeecgBoot PasswordUtil.encrypt:
    plaintext = the password to encrypt (= the plaintext login password)
    password = used as PBE key derivation input (= the username)  
    salt = salt string from DB
    """
    pwd_bytes = password.encode('utf-8')  # username
    salt_bytes = salt_str.encode('utf-8')
    plain_bytes = plaintext.encode('utf-8')
    
    # Java PBKDF1-like key derivation for PBEWithMD5AndDES
    # dk0 = MD5(pwd_bytes + salt_bytes) -> 16 bytes -> key(8) + IV(8)
    dk = md5(pwd_bytes + salt_bytes)
    key = dk[:8]
    iv = dk[8:16]
    
    # PKCS5 padding
    pad_len = 8 - (len(plain_bytes) % 8)
    padded = plain_bytes + bytes([pad_len] * pad_len)
    
    # DES-CBC encryption
    encrypted = des_encrypt_cbc(key, iv, padded)
    
    return encrypted.hex()

# Test
username = "13507007984"
password = "123456"
salt = "APDzGLuO"

hash_val = pbe_md5_des(username, password, salt, 1000)
print(f"PasswordUtil.encrypt('{username}', '{password}', '{salt}')")
print(f"= {hash_val}")
