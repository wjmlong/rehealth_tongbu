from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
from cryptography.hazmat.primitives import padding as sym_padding
import hashlib

def md5(data):
    return hashlib.md5(data).digest()

def pbe_md5_and_des(plaintext, password, salt_str):
    pwd_bytes = password.encode('utf-8')
    salt_bytes = salt_str.encode('utf-8')
    plain_bytes = plaintext.encode('utf-8')
    dk = md5(pwd_bytes + salt_bytes)
    key = dk[:8]
    iv = dk[8:16]
    padder = sym_padding.PKCS7(8).padder()
    padded = padder.update(plain_bytes) + padder.finalize()
    cipher = Cipher(algorithms.DES(key), modes.CBC(iv))
    encryptor = cipher.encryptor()
    encrypted = encryptor.update(padded) + encryptor.finalize()
    return encrypted.hex()

username = "13507007984"
password = "123456"
salt = "APDzGLuO"
hash_val = pbe_md5_and_des(password, username, salt)
print("HASH=" + hash_val)
