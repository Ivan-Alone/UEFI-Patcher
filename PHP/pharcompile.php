<?php
$files = array('bin/patcher.php', 'bin/PatcherLib.php', 'bin/patching_table.txt');
$phar = new Phar('UEFIPatcher.phar');
$phar->setDefaultStub($files[0], $files[0]);
foreach($files as $file) {
	$phar[$file] = file_get_contents($file);
}