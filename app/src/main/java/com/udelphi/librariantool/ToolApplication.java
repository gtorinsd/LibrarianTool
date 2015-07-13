package com.udelphi.librariantool;

import android.app.Application;

/**
 * Created by 1 on 12.05.2015.
 */

public class ToolApplication extends Application
{
    // Database
    final public String DatabaseName = "DBLibrary.sqlite";
    final public int DatabaseVerion = 2;

    // Tables
    final public String tblGenres = "\"tblGenres.db\"";
    final public String tblBooks = "\"tblBooks.db\"";
    final public String tblAuthors = "\"tblAuthors.db\"";
    final public String tblClients = "\"tblClients.db\"";
    final public String tblLibraryTurnover = "\"tblLibraryTurnover.db\"";
    final public String tblLibraryTurnoverArchive = "\"tblLibraryTurnoverArchive.db\"";

    // Import files
    final public String FileSourceGenres ="genres.csv";
    final public String FileSourceAuthors = "authors.csv";
    final public String FileSourceBooks = "books.csv";
    final public String FileSourceClients = "clients.csv";
    final public String FileSourceLibraryTurnover = "LibraryTurnover.csv";
}
