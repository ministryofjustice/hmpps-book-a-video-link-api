-- Remove prisons added in error to the original seed prison data
delete from prison where prison_id = 3 and code = 'AA1';
delete from prison where prison_id = 42 and code = 'ABC';
