import sys
import json

from pyrogram import Client
from pyrogram.raw.functions.contacts import GetContacts

async def main(api_id, api_hash):
    app = Client("my_account", api_id=api_id, api_hash=api_hash)

    async with app:
        try:
            contacts = await app.invoke(GetContacts(hash=0))
            contact_list = []

            for user in contacts.users:
                contact_data = {
                    "id": user.id,
                    "username": f"@{user.username}" if user.username else "-",
                    "first_name": user.first_name or "-",
                    "last_name": user.last_name or "-",
                    "phone": user.phone or "-"
                }
                contact_list.append(contact_data)

            print(json.dumps(contact_list, ensure_ascii=False))

        except Exception as e:
            print(f"Error: {str(e)}", file=sys.stderr)
            sys.exit(1)

if __name__ == "__main__":
    if len(sys.argv) != 3:
        print("Usage: python telegram_contacts.py <api_id> <api_hash>", file=sys.stderr)
        sys.exit(1)

    api_id = int(sys.argv[1])
    api_hash = sys.argv[2]

    app = Client("my_account", api_id=api_id, api_hash=api_hash)
    app.run(main(api_id, api_hash))