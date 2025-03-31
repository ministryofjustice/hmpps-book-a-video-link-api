-- FRC courts to add spaces around the hyphens
update court set description = 'FRC - Bristol' where code = 'FRCBLC';
update court set description = 'FRC - Bournemouth' where code = 'FRCBRC';
update court set description = 'FRC - East Midlands' where code = 'FRCEMC';
update court set description = 'FRC - Peterborough' where code = 'FRCPRC';
update court set description = 'FRC - Plymouth' where code = 'FRCPHC';
update court set description = 'FRC - Thames Valley' where code = 'FRCTVC';
update court set description = 'FRC - West Midlands' where code = 'FRCWMC';

-- RCJ court to correct an odd hyphen character
update court set description = 'RCJ - Court of Appeal Criminal' where code = 'RCJCCC';

-- RCU - nothing obviously wrong with this but updating to ensure no ctrl characters in there
update court set description = 'RCU - Wales and South West' where code = 'RCUWSW';
