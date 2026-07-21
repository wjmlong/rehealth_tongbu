"""CLI tool for BodyUP Cloud administration."""

import argparse
import asyncio
import sys


def create_admin(email: str, password: str):
    async def _run():
        from bodyup_cloud.db.database import init_db, async_session_maker
        from bodyup_cloud.db.repository import UserRepository
        from bodyup_cloud.db.models import User
        from bodyup_cloud.app.auth import hash_password

        await init_db()
        async with async_session_maker() as session:
            repo = UserRepository(session)
            existing = await repo.get_by_email(email)
            if existing:
                print(f"User {email} already exists (role={existing.role})")
                return
            user = User(
                email=email,
                hashed_password=hash_password(password),
                full_name="Admin",
                role="admin",
            )
            session.add(user)
            await session.commit()
            print(f"Admin user created: {email}")

    asyncio.run(_run())


def main():
    parser = argparse.ArgumentParser(prog="bodyup-cli", description="BodyUP Cloud CLI")
    sub = parser.add_subparsers(dest="command")

    p_admin = sub.add_parser("create-admin", help="Create an admin user")
    p_admin.add_argument("--email", required=True)
    p_admin.add_argument("--password", required=True)

    args = parser.parse_args()
    if args.command == "create-admin":
        create_admin(args.email, args.password)
    else:
        parser.print_help()
        sys.exit(1)


if __name__ == "__main__":
    main()
