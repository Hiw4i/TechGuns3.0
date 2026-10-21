"""Puts a weapon into the hotbar of the test world (dev tool).

The test world has no player data, so the client starts with empty hands and
never renders a weapon. This writes a minimal player file with one gun in the
first hotbar slot, so a quick play session shows the view model.

    python tools/seed_test_player.py [techguns3:as50]

Run it again after the client has quit: the integrated server writes the
player back and overwrites the file.
"""

import gzip
import hashlib
import struct
import sys
import uuid
from pathlib import Path

root = Path(__file__).resolve().parents[1]
world = root / "run/saves/New World"
playerdata = world / "playerdata"

USERNAME = "Dev"
GUN = sys.argv[1] if len(sys.argv) > 1 else "techguns3:ak47"
DATA_VERSION = 5023


def java_name_uuid(name: str) -> uuid.UUID:
    """The offline mode UUID of a name, the way java.util.UUID does it."""
    digest = bytearray(hashlib.md5(name.encode("utf-8")).digest())
    digest[6] = (digest[6] & 0x0F) | 0x30
    digest[8] = (digest[8] & 0x3F) | 0x80
    return uuid.UUID(bytes=bytes(digest))


def tag_string(value: str) -> bytes:
    raw = value.encode("utf-8")
    return b"\x08" + struct.pack(">H", len(raw)) + raw


def tag_byte(value: int) -> bytes:
    return b"\x01" + struct.pack(">b", value)


def tag_int(value: int) -> bytes:
    return b"\x03" + struct.pack(">i", value)


def tag_compound(name: str, body: bytes) -> bytes:
    raw = name.encode("utf-8")
    return b"\x0a" + struct.pack(">H", len(raw)) + raw + body + b"\x00"


def tag_list(name: str, element_type: int, items: list[bytes]) -> bytes:
    raw = name.encode("utf-8")
    return (b"\x09" + struct.pack(">H", len(raw)) + raw
            + struct.pack(">b", element_type) + struct.pack(">i", len(items))
            + b"".join(items))


def main() -> None:
    playerdata.mkdir(parents=True, exist_ok=True)
    item = tag_compound("", tag_string("id") + tag_string(GUN)
                        + tag_string("count") + tag_byte(1))
    body = (tag_string("DataVersion") + tag_int(DATA_VERSION)
            + tag_list("Inventory", 10, [item])
            + tag_string("SelectedItemSlot") + tag_byte(0))
    data = gzip.compress(tag_compound("", body))
    path = playerdata / f"{java_name_uuid('OfflinePlayer:' + USERNAME)}.dat"
    path.write_bytes(data)
    print(f"wrote {path} with {GUN} in the first hotbar slot")


main()
