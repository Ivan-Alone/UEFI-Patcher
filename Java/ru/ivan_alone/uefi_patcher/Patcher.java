package ru.ivan_alone.uefi_patcher;

import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Patcher {
	public static boolean isMatch(byte[] pattern, byte[] input, int pos) {
		if (pos + pattern.length > input.length) return false;
		for(int i = 0; i< pattern.length; i++) {
			if(pattern[i] != input[pos+i]) {
				return false;
			}
		}
		return true;
	}
	
	public static boolean equals(byte[] bs, byte[] bs2) {
		if (bs2.length != bs.length) return false;
		for (int i = 0; i < bs.length; i++) {
			if (bs[i] != bs2[i]) return false;
		}
		return true;
	}

	public static List<byte[]> explode(byte[] pattern, byte[] input) {
		List<byte[]> l = new LinkedList<byte[]>();
		int blockStart = 0;
		for(int i=0; i<input.length; i++) {
			if(isMatch(pattern, input, i)) {
				l.add(Arrays.copyOfRange(input, blockStart, i));
				blockStart = i+pattern.length;
				i = blockStart;
			}
		}
		l.add(Arrays.copyOfRange(input, blockStart, input.length ));
		return l;
	}
	
	public static byte[] file_get_contents(String fileName) {
		File f = null;
		byte[] bytes = null;
		try {
			f = new File(fileName);
			bytes = new byte[(int) f.length()];
			RandomAccessFile raf = new RandomAccessFile(f, "r");
			raf.readFully(bytes);
			try {
				raf.close();
			} catch (Exception e) {}
		} catch (Exception e) {}
		return bytes;
	}
	
	public static void file_put_contents(String fileName, byte[] bytes) {
		File f = null;
		try {
			f = new File(fileName);
			RandomAccessFile raf = new RandomAccessFile(f, "rw");
			raf.write(bytes);
			try {
				raf.close();
			} catch (Exception e) {}
		} catch (Exception e) {}
	}
	
	public static char byte2char(byte t) {
		return t >= 0 ? (char)t : (char)(256+t);
	}
	
	public static byte char2byte(char t) {
		return (int)t < 128 ? (byte)t : (byte)(t - 256);
	}

	public static Map<Integer, byte[]> biosSettingsAload(String file_bios) {
		Map<Integer, byte[]> settings = new HashMap<Integer, byte[]>();
		
		byte[] allBytes = file_get_contents(file_bios);
		byte[] signature = {
			char2byte('Z'),
			char2byte('U'),
			char2byte('U'),
			char2byte('U'),
			char2byte('F'),
			char2byte('A'),
			char2byte('4'),
			char2byte('4')
		};
		
		List<byte[]> test = explode(signature, allBytes);
		int id = 0;
		int length = test.size();
		for (byte[] bytes : test) {
			if (id + 1 < length) {
				byte[] b = new byte[560];
				byte[] bytes2 = test.get(id+1);
				int timer = 0;
				for (int i = bytes.length-168; i < bytes.length; i++) {
					b[timer] = bytes[i];
					timer++;
				}
				for (int i = 0; i < signature.length; i++) {
					b[timer] = signature[i];
					timer++;
				}				
				for (int i = 0; i < 384; i++) {
					b[timer] = bytes2[i];
					timer++;
				}
				
				settings.put(id, b);
			}
			
			id++;
		}
		
		return settings;
	}
	
	public static int[] getLeadingConfigs(Map<Integer,byte[]> settings) {
		int[] pos = {-1, -1};
		int[][] array = new int[6][settings.size()];

		Iterator<Integer> it = settings.keySet().iterator();
		
		int idt = 0;
		while (it.hasNext()) {
			byte[] bytes = settings.get(it.next());
			array[0][idt] = (int)byte2char(bytes[4]);
			array[1][idt] = (int)byte2char(bytes[7]);
			array[2][idt] = (int)byte2char(bytes[35]);
			array[3][idt] = (int)byte2char(bytes[36]);
			array[4][idt] = (int)byte2char(bytes[37]);
			array[5][idt] = (int)byte2char(bytes[38]);
			idt++;
		}
		
		int[][] finder = new int[2][6];
		int finderC = 0;
		for (int[] values : array) {
			Map<Integer, Map<Integer, Integer>> block_test = new HashMap<Integer, Map<Integer, Integer>>();
			int cfgId = 0;
			for (int val : values) {
				if (block_test.get(val) == null) {
					block_test.put(val, new HashMap<Integer, Integer>());
				}
				block_test.get(val).put(block_test.get(val).size(), cfgId);
				cfgId++;
			}
			Iterator<Integer> blet = block_test.keySet().iterator();
			while (blet.hasNext()) {
				Map<Integer, Integer> tests = block_test.get(blet.next());
				if (tests.size() == 2) {
					finder[0][finderC] = tests.get(0);
					finder[1][finderC] = tests.get(1);
					finderC++;
				}
			}
		}

		int _1 = equalsMore(finder[0]);
		int _2 = equalsMore(finder[1]);

		if (_1 != -1 && _2 != -1 && _1 != _2) pos = new int[]{_1, _2};

		return pos;
	}
	
	public static int equalsMore(int[] array) {
		Map<Integer, Integer> new_test = new HashMap<Integer, Integer>();
		for (int dimm : array) {
			if (new_test.get(dimm) == null) {
				new_test.put(dimm, 0);
			}
			new_test.put(dimm, new_test.get(dimm)+1);
		}

		int max = 0;
		Iterator<Integer> it = new_test.keySet().iterator();
		while (it.hasNext()) {
			max = Math.max(max, new_test.get(it.next()));
		}
		
		it = new_test.keySet().iterator();
		while (it.hasNext()) {
			int key = it.next();
			int val = new_test.get(key);
			if (val == max) {
				return key;
			}
		}
		return -1;
	}

	public static int toInt(byte[] lst) {
		return Integer.parseInt(new String(lst).trim());
	}
	
	public static Map<Integer,byte[]> loadPatchesMap(String patches) {
		InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream(patches);
		try {
			byte[] data = new byte[is.available()];
			int c = 0;
			while (is.available() != 0) {
				data[c] = char2byte((char)is.read());
				c++;
			}
			Map<Integer,byte[]> patches_map = new HashMap<Integer,byte[]>();
			List<byte[]> test = explode(new byte[]{char2byte('\n')}, data);
			
			for (byte[] patch : test) {
				if (patch.length > 0) {
					try {
						List<byte[]> patch_prop = explode(new byte[]{char2byte(':')}, patch);
						List<byte[]> patch_data = explode(new byte[]{char2byte(',')}, patch_prop.get(1));
						byte[] patch_info = new byte[patch_data.size()];
						int tmr = 0;
						for (byte[] pdd : patch_data) {
							patch_info[tmr] = char2byte((char)toInt(pdd));
							tmr++;
						}
						patches_map.put(toInt(patch_prop.get(0)), patch_info);
					} catch (Exception e) {
						e.printStackTrace();
					};
				}
			}

			return patches_map;
		} catch (Exception e1) {}
		return null;
	}
	
	public static void applyPatches(String bios, String patches_map_) {
		System.out.println("Loaded UEFI file: "+bios);
		System.out.println("Patching map: in-JAR "+patches_map_);
		System.out.println();

		System.out.println("Loading UEFI file to RAM...");
		byte[] bios_text = file_get_contents(bios);
		
		System.out.println("Parsing UEFI settings...");
		Map<Integer,byte[]> settings = biosSettingsAload(bios);
		Map<Integer,byte[]> settings_remap = new HashMap<Integer,byte[]>(settings.size());
		for (int i = 0; i < settings.size(); i++) {
			byte[] copy = new byte[settings.get(i).length];
			for (int x = 0; x < settings.get(i).length; x++) {
				copy[x] = settings.get(i)[x];
			}
			settings_remap.put(i, copy);
		}
		
		System.out.println("Searching key bytes in settings...");
		int[] leading = getLeadingConfigs(settings);
		
		if (leading[0] == -1 || -1 == leading[1]) {
			System.out.println("Error: can't find key bytes in your UEFI dump. Load other UEFI dump...");
			return;
		}

		System.out.println("Loading patches map...");
		Map<Integer,byte[]> patches_map = loadPatchesMap(patches_map_);
		if (patches_map == null) {
			System.out.println("Error: can't load patches map...");
			return;
		}
		

		System.out.print("Patching UEFI settings...");
		for (int id = 0; id < settings_remap.size(); id++) {
			Iterator<Integer> keys = patches_map.keySet().iterator();
			while (keys.hasNext()) {
				int byteId = keys.next();
				byte[] remapData = patches_map.get(byteId);
				byte ids = remapData[remapData.length == 1 ? 0 : ((id == leading[0] || id == leading[1]) ? 1 : 0)];
				byte[] dt = settings_remap.get(id);
				dt[byteId] = ids;
				settings_remap.put(id, dt);
			}
			System.out.print(".");
		}
		System.out.println();

		System.out.print("Applying patch to UEFI...");
		for (int id = 0; id < settings.size(); id++) {
			bios_text = str_replace_once(settings.get(id), settings_remap.get(id), bios_text);
			System.out.print(".");
		}
		System.out.println();
		
		String[] path = bios.replaceAll("\\\\", "/").split("/");
		String _path = "";
		for (int i = 0; i < path.length-1; i++) {
			_path += path[i] + "/";
		}
		System.out.println("Writing fixed UEFI to "+_path+"patched_"+path[path.length-1]+"...");
		file_put_contents(_path+"patched_"+path[path.length-1], bios_text);
		System.out.println("Patching done...");
	}
	
	public static byte[] str_replace_once(byte[] bs, byte[] bs2, byte[] bios_text) {
		if (equals(bs, bs2)) return bios_text;

		List<byte[]> broken = explode(bs, bios_text);
		byte[] data = new byte[bios_text.length + (bs2.length - bs.length)];
		int map_indexation = 0;

		for (int id = 0; id < broken.get(0).length; id++) {
			data[map_indexation] = broken.get(0)[id];
			map_indexation++;
		}
		
		for (int id = 0; id < bs.length; id++) {
			data[map_indexation] = bs2[id];
			map_indexation++;
		}
		
		for (int id = 1; id < broken.size(); id++) {
			for (int id1 = 0; id1 < broken.get(id).length; id1++) {
				data[map_indexation] = broken.get(id)[id1];
				map_indexation++;
			}
			if (id + 1 < broken.size()) {
				for (int id1 = 0; id1 < bs.length; id1++) {
					data[map_indexation] = bs[id1];
					map_indexation++;
				}
			}
		}

		return data;
	}

	public static void main(String[] args) {
		if (args.length < 1) {
			System.out.println("Error: UEFI-file wasn't loaded!\n    Usage: java -jar UEFIPatcher.jar \"C:\\bios\\bios_file_name.bin\"");
		} else if (!new File(args[0]).exists()) {
			System.out.println("Error: UEFI-file don't exists!");
		} else if (new File(args[0]).isDirectory()) {
			System.out.println("Error: UEFI-file can't be an directory!");
		} else {
			applyPatches(args[0], "patching_table.txt");
		}
	}
}