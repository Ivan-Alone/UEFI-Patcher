# UEFI-Patcher
Patcher for restoring UEFI BIOS to factory default. With this patcher you can fix damaged UEFI BIOS on your Digma Citi 1803!

Usage:

```java -jar UEFIPatcher.jar "path/to/bios.bin"```

or

```php UEFIPatcher.phar "path/to/bios.bin"```

You'll receive file "path/to/patched_bios.bin" that you can write with programming circuit to your Flash-chip (W25Q64FWSIG)

**Note**: after firmware flashing go to UEFI (press Del key on keyboard when tablet is booting) and go to:

*Chipset -> North Bridge -> Memory Configuration Options*

And set up next parameters to:

*Frequency A selection: 1600*

*LPDDR3 Chip: 2 Ranks*

If you not set up it, you'll can use only 2 GB RAM.
