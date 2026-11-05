# Backup, restore and delete-all: integrity test steps

Status: **NOT TESTED**. Nothing below has been run; the steps are for the owner on a phone. Run them on a **test profile** with a few throwaway contacts, never on the profile you use. Context: [DEVELOPMENT_STATUS.md](DEVELOPMENT_STATUS.md), "Backup, app lock and delete all (M3)". The screens are Settings > "Veritabanı ve parola" > "Parola, yedek ve silme".

What the screens do: "Yedek al" runs upstream's database export (stop chat, archive, Android file picker, start chat). "Yedekten geri yükle" runs upstream's import (file picker, replace confirmation, delete current storage, import archive, "restart the app" message). "Tüm verileri sil" runs upstream's delete database and then removes Veil's own device-local lists (chat pins and any other `veil_` list). Veil adds only the questions that come first and the wording.

## Setup

1. Fresh install (or a test profile), accept the setup, create a profile with a display name.
2. Connect with a second device or profile and exchange a few messages, a photo and a file. Pin one chat (long press on the avatar).
3. Note the number of contacts, the last three messages of one chat and the pinned chat.

## T1. Backup refused without a passphrase

1. With the database still on the random initial passphrase (nothing set yet), tap "Yedek al".
2. Expected: Veil says a passphrase is needed first; "Parola koy" opens upstream's passphrase screen; no file picker opens; no archive is made.
3. Set a passphrase. Write it down for this test.

## T2. Backup

1. Tap "Yedek al", read the explanation, confirm.
2. Expected: the chat stops briefly, the Android file picker opens, you choose a folder on the phone (not a cloud service) and save. The chat starts again.
3. Expected: a file `simplex-chat.<time>.zip` exists where you saved it. Note its size.

## T3. Reset and restore (the main integrity test)

1. Settings > "Tüm verileri sil", confirm both dialogs. Expected: upstream's "restart the app" message. Restart the app.
2. Expected: Veil returns to the first-run setup (story, Orbot, name, ...). The pin is gone. Chats, contacts and files are gone.
3. Do not create a profile. Look for the way to restore: if the setup has no restore entry, create a throwaway profile first, then Settings > "Yedekten geri yükle". (Record which of the two is needed: this is part of the result.)
4. Read the Veil warning (old backup, same backup on two devices), choose the file, confirm upstream's replace question.
5. Expected: "database imported" message, then a restart request. Restart; if asked, enter the backup's passphrase.
6. Check: same number of contacts, same last messages, the photo and the file open, sending a message to a restored contact works (needs Tor). Record whether the pin came back (it should NOT: pins are not part of the archive).

## T4. Wrong passphrase

1. Repeat T3 steps 4 and 5, but enter a wrong passphrase when asked.
2. Expected: the database error screen refuses it, nothing is opened or overwritten, a correct passphrase afterwards still works. Record the exact screens and texts.

## T5. Truncated file

1. Copy the backup zip and cut it in half (for example with a file manager that can split, or on a computer: `head -c <half the size> backup.zip > cut.zip`, then copy `cut.zip` to the phone).
2. Restore from `cut.zip` (T3 steps 4 and 5).
3. Expected: an error is shown (import failed or imported with errors) and the app is not left in a state you cannot recover from: after a restart you can still restore the intact backup. Record what happens to the current data before the failed import: **upstream deletes the current storage before it imports the archive**, so a failed import of a bad file may leave an empty database. This is the point of the test; if it does, the warning text must say to take a backup first (it does) and the result must be recorded here.

## T6. Old backup (connection damage)

1. After T2, send and receive 5 more messages with a contact.
2. Restore the T2 backup.
3. Expected (SimpleX behavior, not Veil): messages after the backup are gone, and the contact connection may stop working or show a decryption error until it is repaired ("Gelişmiş" > fix connection) or reconnected. Record what is seen. Never open the same backup on two devices at the same time.

## T7. Delete all data

1. Pin a chat, create a second profile with a pin, then "Tüm verileri sil".
2. After the restart and a new profile: no pin appears on any chat (the profile id is reused, so a leftover pin would show up here).
3. Expected: the lock passcode and app settings are still there (they are not deleted).
4. Expected: with "Cihazdaki dosyaları şifrele" on, it is a locked switch (cannot be turned off).

## T8. Self-destruct passcode (separate from delete all; use only a test profile)

1. App lock with Veil parolası on. Settings > "Gelişmiş" > "Kendini imha parolası": the warning appears first; continue; set a self-destruct passcode on upstream's lock page.
2. Lock the app, enter the self-destruct passcode. Expected: all data is deleted and an empty profile opens (upstream behavior).
3. Check the pin list is gone as in T7.

Results go into this file with the date, phone model and Android version when the owner has run them.
