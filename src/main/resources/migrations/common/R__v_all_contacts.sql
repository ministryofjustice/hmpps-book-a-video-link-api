CREATE OR REPLACE VIEW v_all_contacts
    AS
        select 'COURT' as contact_type, c.code, cc.name, cc.position, cc.email, cc.telephone, cc.primary_contact
        from court c, court_contact cc
        where cc.court_id = c.court_id
    UNION
        select 'PROBATION' as contact_type, pt.code, ptc.name, ptc.position, ptc.email, ptc.telephone, ptc.primary_contact
        from probation_team pt, probation_team_contact ptc
        where ptc.probation_team_id = pt.probation_team_id
    UNION
        select 'PRISON' as contact_type, p.code, pc.name, pc.position, pc.email, pc.telephone, pc.primary_contact
        from prison p, prison_contact pc
        where pc.prison_id = p.prison_id
