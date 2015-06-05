This framework have possibility become a open storage have some function below.

  * API to access to the storage from various applications via storage driver.
  * API to join other storage resources that is implemented storage driver interface dynamically (Various local devices, FTP, Google file system, etc. Also this framework installed other places of course).
  * API to read/write to data that is stored in the storage more faster.
  * Reduce and reuse a computer resources.
  * Saved data split some data block (sector) and store the storage by mirroring or striping architecture implementation as a RAID technorogy.

We have been planning make various applications onto it.

Now, we making the storage by H2 database as reference implementation.
And as first application, we planning make a console application for operation and management the storage.

Have fun!