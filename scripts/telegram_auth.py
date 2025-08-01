import asyncio
from pyrogram import Client

async def main(api_id, api_hash, session_name="my_account"):
    async with Client(session_name, api_id=api_id, api_hash=api_hash) as app:
        print("Авторизация прошла успешно.")
        await app.stop()

if __name__ == "__main__":
    import sys
    if len(sys.argv) < 3:
        print("Usage: python telegram_auth.py <api_id> <api_hash>")
        sys.exit(1)

    api_id = int(sys.argv[1])
    api_hash = sys.argv[2]

    asyncio.run(main(api_id, api_hash))