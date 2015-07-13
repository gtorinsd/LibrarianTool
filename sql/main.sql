SELECT 
B.rowid as _id, 
B.Name as BookName, 
A.Name as AuthorName, 
G.Name as GenreName, 
B.Publishing as Publishing, 
B.BookEditionYear as BookEditionYear, 
B.Photo as Photo, 
B.Comments as Comments, 
T.Client_ID as Client_ID
FROM 
"tblBooks.db" B
LEFT OUTER JOIN "tblAuthors.db" A ON B.Author_ID = A._ID
LEFT OUTER JOIN "tblGenres.db" G ON B.Genre_ID = G._ID
LEFT OUTER JOIN "tblLibraryTurnover.db" T ON B._ID = T.Book_ID 
