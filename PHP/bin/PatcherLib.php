<?php
	function _dir() {
		$ccc = explode('/',__DIR__);
		$_cc = count($ccc);
		unset($ccc[0]);
		unset($ccc[1]);
		unset($ccc[$_cc-1]);
		unset($ccc[$_cc-2]);
		return implode('/',$ccc);
	}

	function applyPatches($bios, $patches_map, $mat) {
		echo "Loaded UEFI file: ".($mat == null ? basename($bios) : $mat).PHP_EOL;
		echo "Patching map: in-PHAR ".$patches_map.PHP_EOL .PHP_EOL;
		
		echo "Loading UEFI file to RAM...".PHP_EOL;
		$bios_text = file_get_contents($bios);
		echo "Parsing UEFI settings...".PHP_EOL;
		$settings = biosSettingsAload($bios);
		echo "Searching key bytes in settings...".PHP_EOL;
		$leading = getLeadingConfigs($settings);
		
		if ($leading[0] == -1 || -1 == $leading[1]) {
			echo "Error: can't find key bytes in your UEFI dump. Load other UEFI dump...".PHP_EOL;
			return;
		}
		
		echo "Loading patches map...".PHP_EOL;
		$patches_map = loadPatchesMap('bin/'.$patches_map);
		
		$settings_remap = $settings;
		
		echo "Patching UEFI settings...";
		for ($id = 0; $id < count($settings); $id++) {
			foreach ($patches_map as $byteId => $remapData) {
				$ids = !is_array($remapData) ? $remapData : ($remapData[($id == $leading[0] || $id == $leading[1]) ? 1 : 0]);
				@$settings_remap[$id][$byteId] = chr((int)$ids);
			}
			echo ".";
		}
		echo PHP_EOL;
	
		echo "Applying patch to UEFI...";
		foreach ($settings as $id => $bytes) {
			$bios_text = str_replace_once($bytes, $settings_remap[$id], $bios_text);
			echo ".";
		}
		echo PHP_EOL;
		
		$path = explode("/", str_replace('\\', '/', $bios));
		$_path = '';
		for ($i = 2; $i < count($path)-1; $i++) {
			$_path .= $path[$i] . "/";
		}
		$mma = pathinfo($mat);
		echo "Writing fixed UEFI to ".($mat == null ? "patched_".$path[count($path)-1] : $mma['dirname']."/patched_".$path[count($path)-1])."...".PHP_EOL;
		file_put_contents('file://'.$_path.'patched_'.$path[count($path)-1], $bios_text);
		echo "Patching done!".PHP_EOL;
	}
	
	function loadPatchesMap($filename) {
		$patches = explode("\n", file_get_contents($filename));
		$patches_list = array();
		foreach($patches as $patch) {
			$patch_prop = explode(':',$patch);
			$patch_data = explode(',',$patch_prop[1]);
			$patches_list[trim($patch_prop[0])] = count($patch_data) > 1 ? $patch_data : trim($patch_data[0]);
		}
		
		return $patches_list;
	}
	
	function biosSettingsAload($bios) {
		$settings = array();
		$signature = 'ZUUUFA44';
		$bios = explode($signature, file_get_contents($bios));
		$length = count($bios);
		foreach ($bios as $id => $bytes) {
			if ( $id + 1 < $length) {
				$data = substr($bytes, strlen($bytes)-168, 168).$signature.substr($bios[$id + 1], 0, 384);
				if (strlen($data) == 560) {
					$settings[] = $data;
				}
			}
		}
		return $settings;
	}
	
	function getLeadingConfigs($settings) {
		$array = array();
		foreach ($settings as $bytes) {
			$array[0][] = ord($bytes[4]);
			$array[1][] = ord($bytes[7]);
			$array[2][] = ord($bytes[35]);
			$array[3][] = ord($bytes[36]);
			$array[4][] = ord($bytes[37]);
			$array[5][] = ord($bytes[38]);
		}

		$finder = array();
		foreach ($array as $values) {
			$block_test = array();
			$cfgId = 0;
			foreach ($values as $val) {
				$block_test[$val][] = $cfgId;
				$cfgId++;
			}
			foreach ($block_test as $tests) {
				if (count($tests) == 2) {
					$finder[0][] = $tests[0];
					$finder[1][] = $tests[1];
					break;
				}
			}
		}
		
		$_1 = equalsMore($finder[0]);
		$_2 = equalsMore($finder[1]);
		if (count($finder) == 2 && $_1 != -1 && $_2 != -1 && $_1 != $_2) return array($_1, $_2);
		return array(-1, -1);
	}
	
	function str_replace_once($search, $replace, $text) { 
		$pos = strpos($text, $search); 
		return $pos!==false ? substr_replace($text, $replace, $pos, strlen($search)) : $text; 
	} 
	
	function equalsMore($array) {
		$new_test = array();
		foreach ($array as $dimm) {
			@$new_test[$dimm]++;
		}
		$max = max($new_test);
		foreach ($new_test as $r => $v) {
			if ($v == $max) {
				return $r;
			}
		}
		return -1;
	}
	__HALT_COMPILER();
	