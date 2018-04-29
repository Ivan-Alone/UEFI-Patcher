<?php
	include 'bin/PatcherLib.php';
	if (count($argv) < 2) {
		echo "Error: UEFI-file wasn't loaded!\n    Usage: php UEFIPatcher.phar \"C:\\bios\\bios_file_name.bin\"".PHP_EOL;
	} else {
		$path = 'file://';
		$mod = null;
		if ((@$argv[1][1] == ':' && ((ord(@$argv[1][0]) >= 65 && ord(@$argv[1][0]) <= 90) || (ord(@$argv[1][0]) >= 97 && ord(@$argv[1][0]) <= 122))) || @$argv[1][0] == '\\' || @$argv[1][0] == '/') {
			$path .= $argv[1];
			$mod = $argv[1];
		} else {
			if (substr($argv[1], 0, 3) == '../' || substr($argv[1], 0, 3) == '..\\' || substr($argv[1], 0, 3) == '.\\' || substr($argv[1], 0, 3) == './') {
				$mod = $argv[1];
			}
			$path .= _dir().'/'.$argv[1];
		}
		
		if (!file_exists($path)) {
			echo "Error: UEFI-file isn't exists!".PHP_EOL;
		} else if (is_dir($path)) {
			echo "Error: UEFI-file can't be an directory!".PHP_EOL;
		} else {
			applyPatches($path, 'patching_table.txt', $mod);
		}
	}
	__HALT_COMPILER();
	