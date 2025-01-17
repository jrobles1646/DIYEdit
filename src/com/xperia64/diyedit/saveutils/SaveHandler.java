package com.xperia64.diyedit.saveutils;

import java.io.File;
import java.util.ArrayList;

import com.xperia64.diyedit.FileByteOperations;
import com.xperia64.diyedit.savecrypt.Tachtig;
import com.xperia64.diyedit.savecrypt.Twintig;

// Handles save file operations for DS and RAW Wii saves
public class SaveHandler {

	private byte[] saveFile; // Savefile
	private byte[] mioFile;
	private byte[] sys0File;
	private byte[] sys1File;
	private String s1;
	private String s0;
	private String nam;
	private DSSave d;
	private WiiSave w;

	Tachtig ta;
	Twintig tw;
	String tapath;
	// I heard u like SaveHandlers so I created some SaveHandlers within ur
	// SaveHandler
	SaveHandler games;
	SaveHandler records;
	SaveHandler manga;

	private Boolean IsDSSave() {
		// Support both 16MB and padded 32MB savefiles.
		// dsmode=0-2 (Games, Records, Comics), mionumber=0-89
		return (saveFile.length == 33554432
				|| saveFile.length == 33566720
				|| saveFile.length == 33554554
				|| saveFile.length == 16777216);
	}

	private Boolean IsWiiSave() {
		// GDATA/RDATA/MDATA
		return (saveFile.length == 4719808
				|| saveFile.length == 591040
				|| saveFile.length == 1033408);
	}

	private Boolean IsCompressedWiiSave() {
		return (saveFile.length == 6438592);
	}

	public SaveHandler(String savename) {
		saveFile = FileByteOperations.read(savename);
		nam = savename;
		if (IsDSSave() || nam.endsWith(".dsv")) // And dsv in particular I guess?
		{
			// This is a NDS savefile
			d = new DSSave(saveFile);

		} else if (IsWiiSave()) {
			// This is a Wii savefile
			s1 = savename.substring(0, savename.lastIndexOf(File.separator) + 1) + "SYS_0";
			s0 = savename.substring(0, savename.lastIndexOf(File.separator) + 1) + "SYS_1";
			if (new File(s1).exists() && new File(s0).exists()) {
				sys0File = FileByteOperations.read(s0);
				sys1File = FileByteOperations.read(s1);
			}
			w = new WiiSave(saveFile, sys0File, sys1File);

		} else if (IsCompressedWiiSave()) {
			// This is a compressed/encrypted Wii save
			Tachtig ta = new Tachtig(saveFile);
			ta.do_file_header();
			ta.do_backup_header();

			for (int i = 0; i < ta.n_files; i++)
				ta.do_file();

			ta.do_sig();
			ta.write_permissions();
			tapath = ta.mPath.toString() + File.separator + ta.titleid;
			games = new SaveHandler(tapath + File.separator + "GDATA");
			records = new SaveHandler(tapath + File.separator + "RDATA");
			manga = new SaveHandler(tapath + File.separator + "MDATA");

		}
	}

	public byte[] getMio(int mode, int mionumber) // Filename is the save file name
	{
		if (IsWiiSave()) {
			return w.getMioList(mode).get(mionumber);
		} else if (IsDSSave()) {
			return d.getMioList(mode).get(mionumber);
		} else if (IsCompressedWiiSave()) {
			switch (mode) {
				case 0:
					return games.getMio(0, mionumber);
				case 1:
					return records.getMio(1, mionumber);
				case 2:
					return manga.getMio(2, mionumber);
				default:
					return null;
			}
		} else {
			return null;
		}
	}

	public void setMio(String mioname, int mionumber) // Filename is the save file name, mioname is the mio filename
	{
		// saveFile=fbo.read(filename);
		mioFile = FileByteOperations.read(mioname);
		System.out.println(mioFile.length);
		if (mioFile.length != 65536 && mioFile.length != 8192 && mioFile.length != 14336) {
			System.out.println("Not a mio file!");
			return;
		}
		if (IsDSSave()) {
			// It's a ds save!
			d.setMio(mionumber, mioFile);
			saveFile = d.saveChanges();
		} else if (IsWiiSave() && new File(s0).exists() && new File(s1).exists()) {
			// It's a wii save!
			w.setMio(mionumber, mioFile);
			ArrayList<byte[]> b = w.saveSetChanges();
			saveFile = b.get(0);
			sys0File = b.get(1);
			sys1File = b.get(2);
		} else if (IsCompressedWiiSave()) {
			// It's a compressed Wii save!
			switch (mioFile.length) {
				case 65536:
					games.setMio(mioname, mionumber);
					manga.w.sys0 = manga.sys0File = records.sys0File = records.w.sys0 = games.sys0File;
					manga.w.sys1 = manga.sys1File = records.sys1File = records.w.sys1 = games.sys1File;
					// Sync the sys files.
					/*
					 * records.sys0File=games.sys0File;
					 * records.sys1File=games.sys1File;
					 * records.w.sys0=games.sys0File;
					 * records.w.sys1=games.sys1File;
					 * manga.sys0File=games.sys0File;
					 * manga.sys1File=games.sys1File;
					 * manga.w.sys0=games.sys0File;
					 * manga.w.sys1=games.sys1File;
					 */
					break;
				case 8192:
					records.setMio(mioname, mionumber);
					manga.w.sys0 = manga.sys0File = games.sys0File = games.w.sys0 = records.sys0File;
					manga.w.sys1 = manga.sys1File = games.sys1File = games.w.sys1 = records.sys1File;
					/*
					 * games.sys0File=records.sys0File;
					 * games.sys1File=records.sys1File;
					 * games.w.sys0=records.sys0File;
					 * games.w.sys1=records.sys1File;
					 * manga.sys0File=games.sys0File;
					 * manga.sys1File=games.sys1File;
					 * manga.w.sys0=games.sys0File;
					 * manga.w.sys1=games.sys1File;
					 */
					break;
				case 14336:
					manga.setMio(mioname, mionumber);
					games.w.sys0 = games.sys0File = records.sys0File = records.w.sys0 = manga.sys0File;
					games.w.sys1 = games.sys1File = records.sys1File = records.w.sys1 = manga.sys1File;
					/*
					 * games.sys0File=manga.sys0File;
					 * games.sys1File=manga.sys1File;
					 * games.w.sys0=records.sys0File;
					 * games.w.sys1=records.sys1File;
					 * records.sys0File=games.sys0File;
					 * records.sys1File=games.sys1File;
					 * records.w.sys0=games.sys0File;
					 * records.w.sys1=games.sys1File;
					 */
					break;
			}
		}
	}

	public void deleteMio(int mode, int mionumber) {
		if (IsDSSave()) {
			// It's a ds save!
			d.deleteMio(mode, mionumber);
			saveFile = d.saveChanges();
		} else if (IsWiiSave() && new File(s0).exists() && new File(s1).exists()) {
			// It's a wii save!
			w.deleteMio(mode, mionumber);
			ArrayList<byte[]> b = w.saveSetChanges();
			saveFile = b.get(0);
			sys0File = b.get(1);
			sys1File = b.get(2);
		} else if (IsCompressedWiiSave()) {
			// It's a compressed Wii save!
			switch (mioFile.length) {
				case 65536:
					games.deleteMio(mode, mionumber);
					manga.w.sys0 = manga.sys0File = records.sys0File = records.w.sys0 = games.sys0File;
					manga.w.sys1 = manga.sys1File = records.sys1File = records.w.sys1 = games.sys1File;
					// Sync the sys files.
					/*
					 * records.sys0File=games.sys0File;
					 * records.sys1File=games.sys1File;
					 * records.w.sys0=games.sys0File;
					 * records.w.sys1=games.sys1File;
					 * manga.sys0File=games.sys0File;
					 * manga.sys1File=games.sys1File;
					 * manga.w.sys0=games.sys0File;
					 * manga.w.sys1=games.sys1File;
					 */
					break;
				case 8192:
					records.deleteMio(mode, mionumber);
					manga.w.sys0 = manga.sys0File = games.sys0File = games.w.sys0 = records.sys0File;
					manga.w.sys1 = manga.sys1File = games.sys1File = games.w.sys1 = records.sys1File;
					/*
					 * games.sys0File=records.sys0File;
					 * games.sys1File=records.sys1File;
					 * games.w.sys0=records.sys0File;
					 * games.w.sys1=records.sys1File;
					 * manga.sys0File=games.sys0File;
					 * manga.sys1File=games.sys1File;
					 * manga.w.sys0=games.sys0File;
					 * manga.w.sys1=games.sys1File;
					 */
					break;
				case 14336:
					manga.deleteMio(mode, mionumber);
					games.w.sys0 = games.sys0File = records.sys0File = records.w.sys0 = manga.sys0File;
					games.w.sys1 = games.sys1File = records.sys1File = records.w.sys1 = manga.sys1File;
					/*
					 * games.sys0File=manga.sys0File;
					 * games.sys1File=manga.sys1File;
					 * games.w.sys0=records.sys0File;
					 * games.w.sys1=records.sys1File;
					 * records.sys0File=games.sys0File;
					 * records.sys1File=games.sys1File;
					 * records.w.sys0=games.sys0File;
					 * records.w.sys1=games.sys1File;
					 */
					break;
			}
		}
	}

	public ArrayList<byte[]> getMios(int mode) {
		if (IsWiiSave()) {
			// It's a wii save!
			return w.getMioList(mode);
		} else if (IsDSSave()) {
			return d.getMioList(mode);
		} else if (IsCompressedWiiSave()) {
			switch (mode) {
				case 0:
					return games.getMios(0);
				case 1:
					return records.getMios(1);
				case 2:
					return manga.getMios(2);
				default:
					return null;
			}
		} else {
			return null;
		}
	}

	public byte[] unlockMedals() {
		d.unlockMedals();
		return d.saveChanges();
		// return new DSSave(saveFile, new byte[0], 0, 0).unlockMedals();
	}

	public byte[] unlockGames() {
		d.unlockGames();
		return d.saveChanges();
		// return new DSSave(saveFile, new byte[0], 0, 0).unlockGames();
	}

	public byte[] unlockRecords() {
		d.unlockRecords();
		return d.saveChanges();
		// return new DSSave(saveFile, new byte[0], 0, 0).unlockRecords();
	}

	public byte[] unlockManga() {
		d.unlockManga();
		return d.saveChanges();
		// return new DSSave(saveFile, new byte[0], 0, 0).unlockManga();
	}

	public void release() {
		if (IsWiiSave()) {
			w.release();
		} else if (IsDSSave()) {
			d.release();
		} else if (IsCompressedWiiSave()) {
			games.release();
			records.release();
			manga.release();
		}
		saveFile = null;
		mioFile = null;
		sys0File = null;
		sys1File = null;
	}

	public void saveChanges() {
		if (IsWiiSave()) {
			FileByteOperations.write(sys0File, s0);
			FileByteOperations.write(sys1File, s1);
			FileByteOperations.write(saveFile, nam);
		} else if (IsDSSave()) {
			FileByteOperations.write(saveFile, nam);
		} else if (IsCompressedWiiSave()) {
			games.saveChanges();
			records.saveChanges();
			manga.saveChanges();
			tw = new Twintig(tapath, nam);
			tw.do_file_header(tw.title_id, null);
			tw.find_files();
			tw.do_backup_header(tw.title_id);
			for (int i = 0; i < tw.n_files; i++)
				tw.do_file(i);

			tw.do_sig();

		}
	}
}
