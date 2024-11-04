insert into prison (prison_id, code, name, enabled, notes, created_by, created_time)
values (114,'ACI', 'Altcourse (HMP & YOI)', false, null, 'TIM', current_timestamp),
       (115,'ASI', 'Ashfield (HMP)', false, null, 'TIM', current_timestamp),
       (116,'DGI', 'Dovegate (HMP)', false, null, 'TIM', current_timestamp),
       (117,'DNI', 'Doncaster (HMP)', false, null, 'TIM', current_timestamp),
       (118,'FBI', 'Forest Bank (HMP)', false, null, 'TIM', current_timestamp),
       (119,'FEI', 'Fosse Way (HMP)', false, null, 'TIM', current_timestamp),
       (120,'FWI', 'Five Wells (HMP)', false, null, 'TIM', current_timestamp),
       (121,'LGI', 'Lowdham Grange (HMP)', false, null, 'TIM', current_timestamp),
       (122,'NLI', 'Northumberland (HMP)', false, null, 'TIM', current_timestamp),
       (123,'OWI', 'Oakwood (HMP)', false, null, 'TIM', current_timestamp),
       (124,'PFI', 'Peterborough (HMP & YOI)', false, null, 'TIM', current_timestamp),
       (125,'PRI', 'Parc (HMP)', false, null, 'TIM', current_timestamp),
       (126,'PYI', 'Parc (YOI)', false, null, 'TIM', current_timestamp),
       (127,'RHI', 'Rye Hill (HMP)', false, null, 'TIM', current_timestamp);

alter sequence if exists prison_prison_id_seq restart with 128;
