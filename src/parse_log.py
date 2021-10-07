#!/usr/bin/env python

import os
import sys
import traceback
import argparse
from datetime import datetime, timedelta
import re
import smtplib
from email.message import EmailMessage
import io


REQUEST_ID = 'Request id: '
REQUEST_ID_LEN = len(REQUEST_ID)

CUSTOM_PARAM = 'custom parameters: '
CUSTOM_PARAM_LEN = len(CUSTOM_PARAM)

RESULT_ID = 'Result id: '
RESULT_ID_LEN = len(RESULT_ID)


class Formatter(argparse.ArgumentDefaultsHelpFormatter,
                argparse.RawDescriptionHelpFormatter):
    pass


def _parse_arguments(desc, args):
    """
    Parses command line arguments
    :param desc:
    :param args:
    :return:
    """
    parser = argparse.ArgumentParser(description=desc,
                                     formatter_class=Formatter)
    parser.add_argument('logdir',
                        help='Directory containing log files')
    parser.add_argument('--logsuffix', default='.log',
                        help='Suffix of log files')
    parser.add_argument('--window', default=30, type=int,
                        help='Number of days to look back from end date')
    parser.add_argument('--omitwalltimes', action='store_true',
                        help='If set, omit the text about walltimes of jobs')
    parser.add_argument('--end_date',
                        help='End date for generating report. If unset '
                             'current time is used. Must be in format '
                             'of YYYY-MM-DD Example: 2021-02-01')
    parser.add_argument('--emails',
                        help='If set, emails will be sent to comma delimited'
                             'email addresses set here')
    parser.add_argument('--smtpserver', default='localhost',
                        help='SMTP server')
    parser.add_argument('--label', default='CDAPS',
                        help='Label used in subject line for email report')
    parser.add_argument('--logprefix', default='communitydetection_')

    return parser.parse_args(args)


def log_file_generator(logdir=None, logprefix=None, logsuffix=None):
    """
    Generator that returns ever log file found in directory
    :param logdir: Directory to examine
    :type logdir: str
    :param logsuffix: return only files with this suffix. If None
                      all files are returned
    :type logsuffix: str
    :return: full path to log file
    :rtype: str
    """
    for entry in os.listdir(logdir):
        if logsuffix is not None and not entry.endswith(logsuffix):
            continue
        if logprefix is not None and not entry.startswith(logprefix):
            continue
        fp = os.path.join(logdir, entry)
        if not os.path.isfile(fp):
            continue
        yield fp

    return None


def add_stats_from_logfile(logfile=None, data_dict=None,
                           err_stream=None):
    """
    Reads thru 'logfile' passed in and builds a dict of jobs where id is
    key and value is a dict with the following fields:

    {'id': id of job,
     'algo': algorithm run,
     'param': any parameters noted in log file,
     'rawdate': raw date from logfile name and log entry,
     'datefromrawdate': datetime derived from 'rawdate',
     'start_time': time since epoch in milliseconds,
     'wall_time': time in milliseconds job took to run,
     'status': status of job,
     'message': any message from job,
     'datefromstart': datetime derived from 'start_time',
    }

    :param logfile:
    :return:
    """

    logfilename = os.path.basename(logfile)
    logfiledate = logfilename[logfilename.index('_')+1:logfilename.index('.')]

    with open(logfile, 'r') as f:
        for line in f:
            if REQUEST_ID in line:
                request_raw = line[line.index(REQUEST_ID)+REQUEST_ID_LEN:]
                split_req = request_raw.split(' ')
                request_id = split_req[0]
                request_algo = split_req[4]
                try:
                    cparam_index = request_raw.rindex(CUSTOM_PARAM)
                    if cparam_index == 0:
                        request_param = None
                    else:
                        request_param = request_raw[cparam_index + CUSTOM_PARAM_LEN:]
                except ValueError:
                    request_param = None
                if request_id in data_dict:
                    err_stream.write('duplicate request id found!!!! ' + request_id + '\n')
                    continue

                request_date = logfiledate + ' ' + line[:line.index(' ')]
                request_dict = {'id': request_id,
                                'algo': request_algo,
                                'param': request_param,
                                'rawdate': request_date,
                                'datefromrawdate': datetime.strptime(request_date,
                                                                     '%Y_%m_%d %H:%M:%S.%f')}
                data_dict[request_id] = request_dict
            if RESULT_ID in line:
                result_raw = line[line.index(RESULT_ID)+RESULT_ID_LEN:]
                split_res = result_raw.split(' ')
                result_id = split_res[0]
                result_start = split_res[3]
                result_walltime = split_res[6]
                result_status = split_res[8]
                result_message = ' '.join(split_res[10:]).rstrip()
                if result_id not in data_dict:
                    err_stream.write('No request matching id:' + result_id + ': found' + '\n')
                    continue
                request_dict = data_dict[result_id]
                request_dict['start_time'] = result_start
                request_dict['wall_time'] = result_walltime
                request_dict['status'] = result_status
                request_dict['message'] = result_message
                request_dict['datefromstart'] = datetime.fromtimestamp(int(float(result_start)/1000.0))


def filter_by_date(data_dict=None, start_date=None, end_date=None):
    """
    Filters jobs using 'datefromrawdate' field to only include ones
    that have a 'datefromrawdate' >= to start_date and < end_date

    :param data_dict:
    :type data_dict: dict
    :param start_date:
    :type start_date: datetime
    :param end_date:
    :type end_date: datetime
    :return:
    """
    filtered_dict = dict()
    for entry in data_dict:
        task = data_dict[entry]
        if task['datefromrawdate'] < start_date or task['datefromrawdate'] >= end_date:
            continue
        filtered_dict[entry] = task
    return filtered_dict


def get_jobs_by_type(data_dict):
    """
    Examines 'algo' and creates new dict where the key is
    the value of 'algo' and value is a list of
    jobs (each one a dict) run with that 'algo'

    :param data_dict:
    :return:
    :rtype: dict
    """
    jobtype_dict = dict()
    for entry in data_dict:
        if data_dict[entry]['algo'] not in jobtype_dict:
            jobtype_dict[data_dict[entry]['algo']] = []
        jobtype_dict[data_dict[entry]['algo']].append(data_dict[entry])
    return jobtype_dict


def get_failed_jobs(jobslist):
    """
    Gets count of jobs who have 'status' of 'failed'

    :param jobslist:
    :type jobslist: list of dict
    :return: count of failed jobs
    :rtype: int
    """
    if jobslist is None:
        return 0
    failedcnt = 0
    for job in jobslist:
        if 'status' in job and job['status'] == 'failed':
            failedcnt += 1
    return failedcnt


def get_min_max_avg_for_jobs(jobslist):
    """
    Given a list of jobs gets the min, max, and average runtime
    with jobs that have a walltime

    :param jobslist:
    :type jobslist: list
    :return: (min, max, and average walltime in milliseconds)
    :rtype: tuple
    """
    max_walltime = None
    min_walltime = None
    total_walltime = 0
    jobcnt = 0

    for job in jobslist:
        if 'wall_time' not in job:
            continue
        wall_time = int(job['wall_time'])
        total_walltime += wall_time
        jobcnt += 1
        if max_walltime is None:
            max_walltime = wall_time
        elif wall_time > max_walltime:
            max_walltime = wall_time
        if min_walltime is None:
            min_walltime = wall_time
        elif wall_time < min_walltime:
            min_walltime = wall_time

    if jobcnt > 0:
        avg_walltime = float(total_walltime)/float(jobcnt)
    else:
        avg_walltime = -1

    return min_walltime, max_walltime, avg_walltime


def send_report_as_email(theargs, report_str, end_date=None):
    """
    If 'theargs.emails' is set this method will send the 'report_str' via
    email

    :param theargs:
    :param report_str:
    :return:
    """
    msg = EmailMessage()
    msg.set_content(report_str)
    msg['Subject'] = theargs.label + ' report ' + str(theargs.window) + ' day report ending ' +\
                     end_date.strftime('%Y-%m-%d %H:%M:%S')
    msg['From'] = 'no_reply@ndexbio-stats.ucsd.edu'
    msg['To'] = re.sub('\\s+', '', theargs.emails).split(',')
    smtp_obj = smtplib.SMTP(theargs.smtpserver)
    smtp_obj.send_message(msg)
    smtp_obj.quit()


def parse_logs(theargs, out_stream=sys.stdout,
               err_stream=sys.stderr, end_date=None):
    """
    Parses all the logs to generate a dict containing all the jobs
    that are then queried to generate a report. The report is
    written to 'out_stream' stream
    :param theargs:
    :param out_stream:
    :param err_stream:
    :return:
    """
    data_dict = {}
    logfiles = [x for x in log_file_generator(os.path.abspath(theargs.logdir))]
    logfiles.sort()
    for logfile in logfiles:
        add_stats_from_logfile(logfile=logfile,
                               data_dict=data_dict,
                               err_stream=err_stream)

    start_date = end_date - timedelta(days=theargs.window)
    filtered_dict = filter_by_date(data_dict, start_date=start_date, end_date=end_date)
    filtered_total_jobs = len(filtered_dict)
    out_stream.write('Date from ' + str(start_date.strftime('%Y-%m-%d %H:%M:%S')) +
                     ' up to, but not including ' + str(end_date.strftime('%Y-%m-%d %H:%M:%S')) + '\n')
    out_stream.write('\tTotal jobs: ' + str(filtered_total_jobs) + '\n')

    jobtype_dict = get_jobs_by_type(filtered_dict)
    for entry in jobtype_dict:
        num_failed_jobs = get_failed_jobs(jobtype_dict[entry])
        out_stream.write('\t\tNumber of ' + entry + ' jobs: ' +
                         str(len(jobtype_dict[entry])))

        if theargs.omitwalltimes is False:
            min_walltime, max_walltime,\
            avg_walltime = get_min_max_avg_for_jobs(jobtype_dict[entry])
            out_stream.write(' (Min=' + str(min_walltime) + 'ms, Max=' +
                             str(max_walltime) + 'ms, Avg=' +
                             str(round(avg_walltime)) + 'ms)')
        if num_failed_jobs > 0:
            out_stream.write(' (' + str(num_failed_jobs) + ' failed)')
        out_stream.write('\n')


def main(args):
    """
    Main entry point for program
    :param args: command line arguments usually :py:const:`sys.argv`
    :return: 0 for success otherwise failure
    :rtype: int
    """
    desc = """
    Parses CDAPS 0.8.0 logs found in <logdir> to generate a summary report
 
    """
    theargs = _parse_arguments(desc, args[1:])
    try:
        out_str = sys.stdout
        if theargs.emails is not None:
            out_str = io.StringIO()

        if theargs.end_date is not None:
            end_date = datetime.fromisoformat(theargs.end_date)
        else:
            end_date = datetime.today()
        parse_logs(theargs, out_str, sys.stderr, end_date=end_date)
        if theargs.emails is not None:
            send_report_as_email(theargs, out_str.getvalue(),
                                 end_date=end_date)
        return 0
    except Exception as e:
        sys.stderr.write('\n\nCaught exception: ' + str(e))
        traceback.print_exc()
        return 2


if __name__ == '__main__':  # pragma: no cover
    sys.exit(main(sys.argv))