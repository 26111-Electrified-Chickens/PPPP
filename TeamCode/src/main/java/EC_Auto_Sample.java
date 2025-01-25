import com.pedropathing.follower.Follower;
import com.pedropathing.localization.Pose;
import com.pedropathing.pathgen.BezierCurve;
import com.pedropathing.pathgen.BezierLine;
import com.pedropathing.pathgen.Path;
import com.pedropathing.pathgen.PathChain;
import com.pedropathing.pathgen.Point;
import com.pedropathing.util.Constants;
import com.pedropathing.util.Timer;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;

import pedroPathing.constants.FConstants;
import pedroPathing.constants.LConstants;


@Autonomous (name = "Sample Auto")
public class EC_Auto_Sample extends OpMode{
    private Follower follower;

    private Intake intake;
    private OuttakeLift outtakeLift;
    private Outtake outtake;
    private Misc misc;
    private int transferRealFSM =0;
    private boolean Collected = false;
    private Timer pathTimer, opmodeTimer;
    private int pathState;
    /** Start Pose of our robot */
    private final Pose startPose = new Pose(7.35, 113.625, Math.toRadians(270));

    /** Scoring Pose of our robot. It is facing the submersible at a -45 degree (315 degree) angle. */
    private final Pose scorePose = new Pose(13, 130, Math.toRadians(315));

    /** Lowest (First) Sample from the Spike Mark */
    private final Pose pickup1Pose = new Pose(24, 121, Math.toRadians(0));

    /** Middle (Second) Sample from the Spike Mark */
    private final Pose pickup2Pose = new Pose(24, 131, Math.toRadians(0));

    /** Highest (Third) Sample from the Spike Mark */
    private final Pose pickup3Pose = new Pose(24, 134, Math.toRadians(24));

    /** Park Pose for our robot, after we do all of the scoring. */
    private final Pose parkPose = new Pose(60, 94, Math.toRadians(270));

    /** Park Control Pose for our robot, this is used to manipulate the bezier curve that we will create for the parking.
     * The Robot will not go to this pose, it is used a control point for our bezier curve. */
    private final Pose parkControlPose = new Pose(60, 120, Math.toRadians(270));

    /* These are our Paths and PathChains that we will define in buildPaths() */
    private Path scorePreload, park;
    private PathChain grabPickup1, grabPickup2, grabPickup3, scorePickup1, scorePickup2, scorePickup3;
    private PathChain[] grabPickups, scorePickups;
    public void pickupsequence(){
        switch (transferRealFSM){
            case 1:
                outtake.setClaw(true);
                if (pathTimer.getElapsedTimeSeconds()>0.6){
                    outtakeLift.LiftToTransferGrab();
                    if (pathTimer.getElapsedTimeSeconds() > .9){
                        transferRealFSM = 2;
                    }
                }
                break;
            case 2:
                outtake.setClaw(false);
                if (pathTimer.getElapsedTimeSeconds()>2.5){
                    outtakeLift.LiftToBucket();
                    if(pathTimer.getElapsedTimeSeconds()>1.5){
                        outtake.pivotToScoreSamp();
                        transferRealFSM = 0;
                    }
                }
                break;
            default:
                break;
        }
    }
    public void buildPaths() {
        scorePreload = new Path(new BezierLine(new Point(startPose), new Point(scorePose)));
        scorePreload.setLinearHeadingInterpolation(startPose.getHeading(), scorePose.getHeading());

        /* Here is an example for Constant Interpolation
        scorePreload.setConstantInterpolation(startPose.getHeading()); */

        /* This is our grabPickup1 PathChain. We are using a single path with a BezierLine, which is a straight line. */
        grabPickup1 = follower.pathBuilder()
                .addPath(new BezierLine(new Point(scorePose), new Point(pickup1Pose)))
                .setLinearHeadingInterpolation(scorePose.getHeading(), pickup1Pose.getHeading())
                .build();

        /* This is our scorePickup1 PathChain. We are using a single path with a BezierLine, which is a straight line. */
        scorePickup1 = follower.pathBuilder()
                .addPath(new BezierLine(new Point(pickup1Pose), new Point(scorePose)))
                .setLinearHeadingInterpolation(pickup1Pose.getHeading(), scorePose.getHeading())
                .build();

        /* This is our grabPickup2 PathChain. We are using a single path with a BezierLine, which is a straight line. */
        grabPickup2 = follower.pathBuilder()
                .addPath(new BezierLine(new Point(scorePose), new Point(pickup2Pose)))
                .setLinearHeadingInterpolation(scorePose.getHeading(), pickup2Pose.getHeading())
                .build();

        /* This is our scorePickup2 PathChain. We are using a single path with a BezierLine, which is a straight line. */
        scorePickup2 = follower.pathBuilder()
                .addPath(new BezierLine(new Point(pickup2Pose), new Point(scorePose)))
                .setLinearHeadingInterpolation(pickup2Pose.getHeading(), scorePose.getHeading())
                .build();

        /* This is our grabPickup3 PathChain. We are using a single path with a BezierLine, which is a straight line. */
        grabPickup3 = follower.pathBuilder()
                .addPath(new BezierLine(new Point(scorePose), new Point(pickup3Pose)))
                .setLinearHeadingInterpolation(scorePose.getHeading(), pickup3Pose.getHeading())
                .build();

        /* This is our scorePickup3 PathChain. We are using a single path with a BezierLine, which is a straight line. */
        scorePickup3 = follower.pathBuilder()
                .addPath(new BezierLine(new Point(pickup3Pose), new Point(scorePose)))
                .setLinearHeadingInterpolation(pickup3Pose.getHeading(), scorePose.getHeading())
                .build();

        /* This is our park path. We are using a BezierCurve with 3 points, which is a curved line that is curved based off of the control point */
        park = new Path(new BezierCurve(new Point(scorePose), /* Control Point */ new Point(parkControlPose), new Point(parkPose)));
        park.setLinearHeadingInterpolation(scorePose.getHeading(), parkPose.getHeading());

        grabPickups = new PathChain[]{grabPickup1, grabPickup2, grabPickup3};
        scorePickups = new PathChain[]{scorePickup1, scorePickup2, scorePickup3};
    }

    private int sampleCounter = 0;
    public void autonomousPathUpdate() {
        switch (pathState) {

            case 0:
                follower.followPath(scorePreload);
                outtake.setClaw(false);
                outtakeLift.LiftToBucket();
                outtake.pivotToScoreSamp();

                setPathState(1);
                break;
            case 1:
                // Preload
                if(pathTimer.getElapsedTimeSeconds()>2) {
                    outtake.setClaw(true);
                    if (pathTimer.getElapsedTimeSeconds() > 3){
                        setPathState(2);
                    }
                }
                break;
            case 2:
                // Loop Begins
                // Outtake ready to transfer & goes to grab a sample
                outtakeLift.LiftToTransferWait();
                outtake.pivotToTransfer();
                follower.followPath(grabPickups[sampleCounter],true);
                setPathState(3);
                break;
            case 3:
                // Intake grabs a sample
                if(!outtakeLift.isBusy()){
                    outtake.setClaw(false);
                    if(!outtake.isClawBusy()){
                        outtake.pivotToPickupBack();
                    }
                }
                if(!follower.isBusy()){
                    if (!Collected){
                        intake.flipDown();
                        intake.ManualExtend();
                    } else if (Collected && !intake.isCorrectColor()){
                        intake.deposit();
                        intake.ManualRetract();
                        if (intake.extendo.getCurrentPosition() < 40){
                            Collected = false;
                        }
                    }
                    if((intake.isCorrectColor() || pathTimer.getElapsedTimeSeconds() > 5) && !Collected){
                        Collected = true;
                        intake.ManualRetract();
                        if(intake.extendo.getCurrentPosition() < 15){
                            intake.deposit();
                                follower.followPath(scorePickups[sampleCounter], true);
                                setPathState(4);
                            }
                        }
                    }

                break;
            case 4:
                // Transfer sequence
                pickupsequence();
                if (transferRealFSM ==0){
                    if(pathTimer.getElapsedTimeSeconds()>5.5) {
                        outtake.setClaw(true);
                        if (pathTimer.getElapsedTimeSeconds() > 8){
                            sampleCounter++;
                            if(sampleCounter < 3) setPathState(2);
                            else{
                                outtakeLift.LiftTarget(900);
                                outtake.pivotToTransfer();
                                follower.followPath(park, false);
                                setPathState(5);
                            }
                        }
                    }
                }
                break;
            case 5:
                if(!follower.isBusy()){
                    outtake.pivotToFront();
                }
                break;
            default:
                break;
        }
    }
    public void setPathState(int pState) {
        pathState = pState;
        pathTimer.resetTimer();
    }
    @Override
    public void init() {
        pathTimer = new Timer();
        opmodeTimer = new Timer();
        opmodeTimer.resetTimer();

        Constants.setConstants(FConstants.class, LConstants.class);
        follower = new Follower(hardwareMap);
        follower.setStartingPose(startPose);
        intake = new Intake(hardwareMap, this);
        outtake = new Outtake(hardwareMap);
        outtakeLift = new OuttakeLift(hardwareMap,this);
        buildPaths();
    }
    @Override
    public void start(){
        intake.initiate();
        outtakeLift.HoldLift();
    }

    private final ToggleButton teamColorButton = new ToggleButton(PoseStorage.isRed);

    @Override
    public void init_loop(){
        teamColorButton.input(gamepad1.dpad_up);

        PoseStorage.isRed = teamColorButton.getVal();

        telemetry.addData("Team Color:", PoseStorage.isRed ? "Red" : "Blue");
        telemetry.update();
    }
    @Override
    public void loop() {

        // These loop the movements of the robot
        follower.update();
        autonomousPathUpdate();

        intake.extendoLoop();
        intake.intakeLoop();
        outtake.loop();
        outtakeLift.HoldLift();
        pickupsequence();

        PoseStorage.CurrentPose = follower.getPose();

        // Feedback to Driver Hub
        telemetry.addData("path state", pathState);
        telemetry.addData("Scored ground samples", sampleCounter);
        telemetry.addData("x", follower.getPose().getX());
        telemetry.addData("y", follower.getPose().getY());
        telemetry.addData("heading", follower.getPose().getHeading());
        telemetry.update();
    }

}
