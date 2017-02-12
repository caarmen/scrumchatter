Scrum Chatter Privacy Policy
============================

Scrum Chatter does not have the `INTERNET` permission, so it cannot send any data over the internet.

Scrum Chatter stores all data you explicitly enter while using the app into a local database *not* accessible by other applications.  This includes:
* team names
* member names
* meeting dates
* speaking times of team members at meetings

Scrum Chatter gives you the possibility to share certain information.  This requires your active manual interaction with the app.
Scrum Chatter cannot transmit this information anywhere silently or without your knowledge.
When you explicitly choose to share this information, you select another app which will transmit the data.
Scrum Chatter provides the information to share to the app you choose, and the app you choose transmits the data to whoever you choose.

Data you can explicitly choose to share, using an external app:
* From a meeting screen, the "Share" icon allows you to share speaking times for team members at one specific meeting.
* From a stats screen, the "Share" icon allows you to share the image of the selected chart.  This depends on the screen.  This can be:
  - column chart: speaking times for members during one meeting
  - line chart: meeting durations over a time period
  - pie charts: average and total speaking time for team members
* From the main screen, the "Share" icon allows you to share all team, member and meeting data in Excel or database format.

In order to be able to share data as files or images, Scrum Chatter must save this data to a file on your device's memory, accessible to other applications.
If you want to delete these files after sharing them, you must use a file browser app installed on your device.
The files are saved to `<sdcard>/Android/data/ca.rmen.android.scrumchatter/files`.

Permissions
-----------

`READ_EXTERNAL_STORAGE`

Scrum Chatter allows you to import a database file which you previously exported.  The `READ_EXTERNAL_STORAGE`
permission is required to be able to read the database file and import its data into the Scrum Chatter
local database.  If the file is anything other than a valid Scrum Chatter database, the import will fail,
and Scrum Chatter will do nothing with the data.  On Marshmallow (6.x) and later versions of Android,
you must first grant this permission to Scrum Chatter before you can select and import a database file.

`WRITE_EXTERNAL_STORAGE`

Sharing the application data as a database or Excel file, and sharing chart images, require saving files to the device's external storage.
The files are saved to the folder dedicated to the app: `<sdcard>/Android/data/ca.rmen.android.scrumchatter/files`.
For Android versions prior to KitKat (4.4), this permission was required to write to this folder.  
For KitKat and later versions, this permission is no longer required.


More questions?
---------------
If you're interested in how Scrum Chatter works, feel free to

* browse the source code: https://github.com/caarmen/scrum-chatter
* submit an issue: https://github.com/caarmen/scrum-chatter/issues

Scrum Chatter is a completely free and open-source app, developed as a hobby on free time by one developer. The developer makes no money off this app, and has zero interest in your personal data.




