import {
  GET,
  GET_LIST,
  PAGE_SIZE,
  POST,
  DELETE,
  PUT,
  encode_id,
} from "../helper.jsx";

/* groups */

const get_groups = (state, query = {}, append = false) =>
  GET_LIST(
    `/group?${new URLSearchParams({ firstResult: 0, maxResults: PAGE_SIZE, sortBy: "id", sortOrder: "asc", ...query })}`,
    state,
    state.api.group.list,
    append,
  );

const create_group = (state, group) =>
  POST(`/group/create`, group, state, state.api.group.create);

const update_group = (state, group_id, group) =>
  PUT(`/group/${encode_id(group_id)}`, group, state, state.api.group.update);

const delete_group = (state, group_id) =>
  DELETE(`/group/${encode_id(group_id)}`, {}, state, state.api.group.delete);

const add_user_to_group = (state, group_id, user_name) =>
  PUT(
    `/group/${encode_id(group_id)}/members/${encode_id(user_name)}`,
    {
      id: group_id,
      userId: user_name,
    },
    state,
    state.api.group.add_user,
  );

const remove_member = (state, group_id, user_name) =>
  DELETE(
    `/group/${encode_id(group_id)}/members/${encode_id(user_name)}`,
    {
      id: group_id,
      userId: user_name,
    },
    state,
    state.api.group.remove_member,
  );

// Groups the given user is a member of (used on the user details page).
const get_user_groups = (state, user_name) =>
  POST(
    "/group",
    {
      member: user_name ?? state.auth.user.id.value,
      firstResult: 0,
      maxResults: 50,
    },
    state,
    state.api.user.group.list,
  );

// Members (users) of the given group (used on the group details page).
const get_group_members = (state, group_id) =>
  GET(
    `/user?memberOfGroup=${encode_id(group_id)}&firstResult=0&maxResults=50`,
    state,
    state.api.group.members,
  );

const group = {
  all: get_groups,
  create: create_group,
  update: update_group,
  delete: delete_group,
  by_member: get_user_groups,
  members: get_group_members,
  add_user: add_user_to_group,
  remove_member,
};

export default group;
