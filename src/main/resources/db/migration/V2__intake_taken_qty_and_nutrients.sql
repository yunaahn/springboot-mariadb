-- 체크 시점에 실제 차감한 수량. 취소할 때 이 값만큼 되돌린다.
ALTER TABLE intake_logs
    ADD COLUMN taken_qty DECIMAL(8,2) NULL COMMENT '체크 시 차감한 수량' AFTER taken_at;

-- 기본 성분 (코드·이름·표준 단위). 권장량/상한 값은 2단계에서 넣는다.
INSERT INTO nutrients (code, name_ko, unit, category) VALUES
    ('VIT_A', '비타민A', 'μg', 'VITAMIN'),
    ('VIT_B1', '비타민B1', 'mg', 'VITAMIN'),
    ('VIT_B2', '비타민B2', 'mg', 'VITAMIN'),
    ('VIT_B6', '비타민B6', 'mg', 'VITAMIN'),
    ('VIT_B12', '비타민B12', 'μg', 'VITAMIN'),
    ('VIT_C', '비타민C', 'mg', 'VITAMIN'),
    ('VIT_D', '비타민D', 'μg', 'VITAMIN'),
    ('VIT_E', '비타민E', 'mg', 'VITAMIN'),
    ('VIT_K', '비타민K', 'μg', 'VITAMIN'),
    ('NIACIN', '나이아신', 'mg', 'VITAMIN'),
    ('FOLATE', '엽산', 'μg', 'VITAMIN'),
    ('BIOTIN', '비오틴', 'μg', 'VITAMIN'),
    ('CALCIUM', '칼슘', 'mg', 'MINERAL'),
    ('MAGNESIUM', '마그네슘', 'mg', 'MINERAL'),
    ('IRON', '철', 'mg', 'MINERAL'),
    ('ZINC', '아연', 'mg', 'MINERAL'),
    ('SELENIUM', '셀레늄', 'μg', 'MINERAL'),
    ('OMEGA3', '오메가3', 'mg', 'ETC'),
    ('LUTEIN', '루테인', 'mg', 'ETC'),
    ('COQ10', '코엔자임Q10', 'mg', 'ETC');
