from Crypto.Cipher import DES
from Crypto.Util.Padding import pad
import hashlib

def md5(data):
    return hashlib.md5(data).digest()

def pbe_md5_and_des(plaintext, password, salt_str):
    """
    Java PBEWithMD5AndDES key derivation (PKCS5 Scheme 1):
    dk = MD5(password_bytes + salt_bytes)  -- 16 bytes for DES (8 key + 8 IV)
    Then DES-CBC encrypt with PKCS5 padding.
    """
    pwd_bytes = password.encode('utf-8')
    salt_bytes = salt_str.encode('utf-8')
    plain_bytes = plaintext.encode('utf-8')
    dk = md5(pwd_bytes + salt_bytes)
    key = dk[:8]
    iv = dk[8:16]
    padded = pad(plain_bytes, 8, style='pkcs7')
    cipher = DES.new(key, DES.MODE_CBC, iv)
    encrypted = cipher.encrypt(padded)
    return encrypted.hex()

username = "13507007984"
password = "123456"
salt = "APDzGLuO"
hash_val = pbe_md5_and_des(password, username, salt)
print("HASH=" + hash_val)
