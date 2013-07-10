ssh-file-uploader
=================

Ssh file uploader v1.0
----------------------------

This Java programme uploads large files with scp and ssh over unreliable internet connections. 
It does this by splitting the large file into small parts and uploading them in parallel over multiple scp connections.
When an upload stalls, it is repeated for that part.

Usage:
----------------------------

SSHFileUploader path_to_configuration file_to_upload path_on_remote_server

Example:
----------------------------

java -jar sshfileuploader.jar /home/jack/tools/sshfileuploader.properties ./photos/backup.zip /mnt/raid/backups/2013-06-13

Example configuration file:

# Start every that many milliseconds the next upload or restart a failed upload
INTERVAL=1000

# Wait at least that many milliseconds after starting a new upload before starting another one 
PAUSE_AFTER_NEW_PROCSS=500

# Maximum number of parallel uploads
MAXPROCESSES=2

# Maximum size of file part to upload in kilobytes. Rule of thumb: the more reliable the connection the larger the value you can pick.
# Chosing large values on bad connections will stall the upload and result in very low speed.
PARTSIZE_KB=64

# Fail at most that many times when merging parts at the target server
MAX_FAILURES=20

# Location of scp binary
scp=/usr/bin/scp

# Location of ssh binary
ssh=/usr/bin/ssh

# SSH login to use at target server for scp and merging
userName=sshuser

# Name or IP of server to upload files to
server=ssh.example.com

# Location for temporary file space
tmpDir=/tmp

# Logging: normal, quiet
logging=normal

# SSH / SCP arguments
sshArgs=-o ConnectTimeout=30

System requirements
--------------------------------

Java 1.6
scp
ssh


FAQ
-------------------------------

Q: Can I specify the password to use for ssh ?
A: No. SSH must be set up to allow non-interactive login (i.e. keyfile)

Q: What is the best setting for MAXPROCESSES and PARTSIZE_KB ?
A: That depends on your connection. Unreliable connections will require smaller PARTSIZE_KBs, a slow ssh handshake will benefit from
larger MAXPROCESSES. The best thing is to try it out.

Q: It hangs
A: Assuming that your network connection to the remote server is not completely dead, make sure you have disabled ControlMaster in
ssh_config. If enabled, it will simulate multiple ssh connections over the same TCP connection which defies the purpose of this programme.

Q: Merging doesn't seem to work
A: You need a bash on the remote server

About
-------------------------------

Author: George Georgovassilis
https://github.com/ggeorgovassilis/ssh-file-uploader
