-- Database Update Script for Multiple Exercise Types
-- Run this against your existing Supabase database

-- 1. Update user_stats table to track different exercise types
ALTER TABLE public.user_stats 
ADD COLUMN total_arm_circles INTEGER DEFAULT 0,
ADD COLUMN total_high_knees INTEGER DEFAULT 0,
ADD COLUMN total_side_reaches INTEGER DEFAULT 0,
ADD COLUMN total_jack_jumps INTEGER DEFAULT 0,
ADD COLUMN total_biceps_curls INTEGER DEFAULT 0,
ADD COLUMN total_shoulder_presses INTEGER DEFAULT 0,
ADD COLUMN total_squats INTEGER DEFAULT 0;

-- 2. Update exercise_sessions table to be more generic
ALTER TABLE public.exercise_sessions 
RENAME COLUMN jumps_completed TO reps_completed;

-- 3. Add a comment to clarify the new structure
COMMENT ON COLUMN public.exercise_sessions.exercise_type IS 'Types: jump, arm_circles, high_knees, side_reach, jack_jumps, biceps_curl, shoulder_press, squat';
COMMENT ON COLUMN public.exercise_sessions.reps_completed IS 'Number of repetitions completed for any exercise type';

-- 4. Create a view for easier stats querying
CREATE OR REPLACE VIEW public.user_exercise_summary AS
SELECT 
    us.user_id,
    us.xp,
    us.level,
    us.exercises_completed,
    us.total_jumps,
    us.total_arm_circles,
    us.total_high_knees,
    us.total_side_reaches,
    us.total_jack_jumps,
    us.total_biceps_curls,
    us.total_shoulder_presses,
    us.total_squats,
    (us.total_jumps + us.total_arm_circles + us.total_high_knees + 
     us.total_side_reaches + us.total_jack_jumps + us.total_biceps_curls + 
     us.total_shoulder_presses + us.total_squats) as total_all_exercises,
    us.created_at,
    us.updated_at
FROM public.user_stats us;

-- 5. Update the existing function to handle new exercise types
CREATE OR REPLACE FUNCTION public.update_user_exercise_stats(
    p_user_id UUID,
    p_exercise_type VARCHAR(50),
    p_reps_completed INTEGER,
    p_xp_earned INTEGER,
    p_session_duration INTEGER DEFAULT 0
)
RETURNS JSON AS $$
DECLARE
    updated_stats RECORD;
BEGIN
    -- Insert the exercise session
    INSERT INTO public.exercise_sessions (
        user_id, 
        exercise_type, 
        reps_completed, 
        xp_earned, 
        session_duration
    ) VALUES (
        p_user_id, 
        p_exercise_type, 
        p_reps_completed, 
        p_xp_earned, 
        p_session_duration
    );
    
    -- Update user stats based on exercise type
    UPDATE public.user_stats 
    SET 
        xp = xp + p_xp_earned,
        level = FLOOR((xp + p_xp_earned) / 100), -- 100 XP per level
        exercises_completed = exercises_completed + 1,
        total_jumps = CASE WHEN p_exercise_type = 'jump' THEN total_jumps + p_reps_completed ELSE total_jumps END,
        total_arm_circles = CASE WHEN p_exercise_type = 'arm_circles' THEN total_arm_circles + p_reps_completed ELSE total_arm_circles END,
        total_high_knees = CASE WHEN p_exercise_type = 'high_knees' THEN total_high_knees + p_reps_completed ELSE total_high_knees END,
        total_side_reaches = CASE WHEN p_exercise_type = 'side_reach' THEN total_side_reaches + p_reps_completed ELSE total_side_reaches END,
        total_jack_jumps = CASE WHEN p_exercise_type = 'jack_jumps' THEN total_jack_jumps + p_reps_completed ELSE total_jack_jumps END,
        total_biceps_curls = CASE WHEN p_exercise_type = 'biceps_curl' THEN total_biceps_curls + p_reps_completed ELSE total_biceps_curls END,
        total_shoulder_presses = CASE WHEN p_exercise_type = 'shoulder_press' THEN total_shoulder_presses + p_reps_completed ELSE total_shoulder_presses END,
        total_squats = CASE WHEN p_exercise_type = 'squat' THEN total_squats + p_reps_completed ELSE total_squats END,
        updated_at = NOW()
    WHERE user_id = p_user_id
    RETURNING *;
    
    -- Get the updated stats
    SELECT * INTO updated_stats 
    FROM public.user_exercise_summary 
    WHERE user_id = p_user_id;
    
    -- Return the updated stats as JSON
    RETURN row_to_json(updated_stats);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 6. Grant execute permission on the function
GRANT EXECUTE ON FUNCTION public.update_user_exercise_stats(UUID, VARCHAR, INTEGER, INTEGER, INTEGER) TO authenticated;

-- 7. Create a function to get user stats (for API)
CREATE OR REPLACE FUNCTION public.get_user_stats(p_user_id UUID)
RETURNS JSON AS $$
DECLARE
    user_stats_json JSON;
BEGIN
    SELECT row_to_json(ues)
    INTO user_stats_json
    FROM public.user_exercise_summary ues
    WHERE ues.user_id = p_user_id;
    
    RETURN user_stats_json;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 8. Grant execute permission
GRANT EXECUTE ON FUNCTION public.get_user_stats(UUID) TO authenticated; 